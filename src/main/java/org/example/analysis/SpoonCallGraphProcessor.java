package org.example.analysis;

import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.reference.CtExecutableReference;
import org.example.model.CallGraph; // Your existing CallGraph model

public class SpoonCallGraphProcessor extends AbstractProcessor<CtMethod<?>> {

    private final CallGraph callGraph = new CallGraph();

    @Override
    public void process(CtMethod<?> method) {
        // Get the fully qualified signature of the caller method
        String callerSignature = method.getSignature(); // Spoon provides signatures directly
        CtType<?> declaringType = method.getDeclaringType();
        if (declaringType != null) {
            callerSignature = declaringType.getQualifiedName() + "." + callerSignature;
        } else {
            // Handle methods not directly in types (e.g., in anonymous classes) - might need refinement
            callerSignature = "UnknownClass." + callerSignature;
        }


        // Find all method calls within this method's body
        final String finalCallerSignature = callerSignature; // Need final variable for lambda
        method.filterChildren(CtInvocation.class::isInstance)
                .forEach(element -> {
                    CtInvocation<?> invocation = (CtInvocation<?>) element;
                    CtExecutableReference<?> executableRef = invocation.getExecutable();

                    if (executableRef != null) {
                        try {
                            // Spoon's type resolution is built-in!
                            CtMethod<?> calledMethod = (CtMethod<?>) executableRef.getDeclaration();
                            if (calledMethod != null) {
                                String calleeSignature = calledMethod.getSignature();
                                CtType<?> calleeDeclaringType = calledMethod.getDeclaringType();
                                if(calleeDeclaringType != null) {
                                    calleeSignature = calleeDeclaringType.getQualifiedName() + "." + calleeSignature;
                                } else {
                                    calleeSignature = "UnknownClass." + calleeSignature;
                                }

                                // Add to your existing CallGraph model
                                callGraph.addCall(finalCallerSignature, calleeSignature);
                            } else {
                                // Could be a call to a library method without source code
                                // Use the reference signature as fallback
                                String calleeSignature = executableRef.getSignature();
                                if (executableRef.getDeclaringType() != null) {
                                    calleeSignature = executableRef.getDeclaringType().getQualifiedName() + "." + calleeSignature;
                                } else {
                                    calleeSignature = "UnknownRef." + calleeSignature;
                                }
                                callGraph.addCall(finalCallerSignature, calleeSignature);
                            }
                        } catch (Exception e) {
                            System.err.println("Spoon WARN: Could not resolve call target for " + executableRef.getSignature() + " in " + finalCallerSignature);
                            // Fallback using the reference
                            String calleeSignature = executableRef.getSignature();
                            if (executableRef.getDeclaringType() != null) {
                                calleeSignature = executableRef.getDeclaringType().getQualifiedName() + "." + calleeSignature;
                            } else {
                                calleeSignature = "UnknownRef." + calleeSignature;
                            }
                            callGraph.addCall(finalCallerSignature, calleeSignature);
                        }
                    }
                });
    }

    // Method to get the result after processing
    public CallGraph getCallGraph() {
        return callGraph;
    }
}
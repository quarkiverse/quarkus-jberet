package io.quarkiverse.jberet.components.deployment.jpa;

import java.util.Map;
import java.util.function.BiFunction;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Jakarta Persistence 4 removed some methods used by {@code org.jberet.jpa.repository.JpaRepository}, which is compiled
 * against Jakarta Persistence 3. The replacements are source compatible, but not binary compatible, so the calls fail
 * with {@link NoSuchMethodError}. This rewrites the method descriptors of these calls to their Jakarta Persistence 4
 * replacements, which take a supertype of the original parameter and return the same type or a subtype of it.
 * <p>
 * Remove once JBeret publishes a {@code jberet-jpa-repository} compiled against Jakarta Persistence 4.
 */
class JpaRepositoryTransformer implements BiFunction<String, ClassVisitor, ClassVisitor> {
    static final String JPA_REPOSITORY = "org.jberet.jpa.repository.JpaRepository";

    private static final Map<String, String> DESCRIPTORS = Map.of(
            "jakarta/persistence/EntityManager.createQuery"
                    + "(Ljakarta/persistence/criteria/CriteriaUpdate;)Ljakarta/persistence/Query;",
            "(Ljakarta/persistence/criteria/CriteriaStatement;)Ljakarta/persistence/Statement;",
            "jakarta/persistence/EntityManager.createQuery"
                    + "(Ljakarta/persistence/criteria/CriteriaQuery;)Ljakarta/persistence/TypedQuery;",
            "(Ljakarta/persistence/criteria/CriteriaSelect;)Ljakarta/persistence/TypedQuery;",
            "jakarta/persistence/criteria/CriteriaBuilder.and"
                    + "([Ljakarta/persistence/criteria/Predicate;)Ljakarta/persistence/criteria/Predicate;",
            "([Ljakarta/persistence/criteria/BooleanExpression;)Ljakarta/persistence/criteria/Predicate;",
            "jakarta/persistence/criteria/CriteriaQuery.where"
                    + "([Ljakarta/persistence/criteria/Predicate;)Ljakarta/persistence/criteria/CriteriaQuery;",
            "([Ljakarta/persistence/criteria/BooleanExpression;)Ljakarta/persistence/criteria/CriteriaQuery;",
            "jakarta/persistence/criteria/CriteriaUpdate.where"
                    + "([Ljakarta/persistence/criteria/Predicate;)Ljakarta/persistence/criteria/CriteriaUpdate;",
            "([Ljakarta/persistence/criteria/BooleanExpression;)Ljakarta/persistence/criteria/CriteriaUpdate;");

    @Override
    public ClassVisitor apply(String className, ClassVisitor classVisitor) {
        return new ClassVisitor(Opcodes.ASM9, classVisitor) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                    String[] exceptions) {
                MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM9, methodVisitor) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor,
                            boolean isInterface) {
                        String replacement = DESCRIPTORS.get(owner + "." + name + descriptor);
                        super.visitMethodInsn(opcode, owner, name, replacement != null ? replacement : descriptor,
                                isInterface);
                    }
                };
            }
        };
    }
}

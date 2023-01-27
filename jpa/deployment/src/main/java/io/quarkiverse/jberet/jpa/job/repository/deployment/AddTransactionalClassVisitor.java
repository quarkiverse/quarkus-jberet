package io.quarkiverse.jberet.jpa.job.repository.deployment;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

public class AddTransactionalClassVisitor extends ClassVisitor {

    private static final String ANNOTATION_TYPENAME = "Ljakarta.transaction.Transactional;";

    public AddTransactionalClassVisitor() {
        super(Opcodes.V11);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {

        //        AnnotationVisitor av = visitAnnotation(
        //                ANNOTATION_TYPENAME,
        //                true);
        super.visit(version, access, name, signature, superName, interfaces);
    }

}

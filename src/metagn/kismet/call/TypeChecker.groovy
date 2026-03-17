package metagn.kismet.call

import groovy.transform.CompileStatic
import metagn.kismet.scope.TypedContext
import metagn.kismet.vm.IKismetObject
import metagn.kismet.vm.Memory

@CompileStatic
abstract class TypeChecker implements KismetCallable, IKismetObject {
	abstract TypedExpression transform(TypedContext context, Expression... args)

	IKismetObject call(Memory c, Expression... args) {
		transform(null, args).instruction.evaluate(c)
	}

	TypeChecker inner() { this }
}

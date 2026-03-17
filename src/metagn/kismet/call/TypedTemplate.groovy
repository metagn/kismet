package metagn.kismet.call

import groovy.transform.CompileStatic
import metagn.kismet.exceptions.CannotOperateException
import metagn.kismet.scope.TypedContext
import metagn.kismet.vm.IKismetObject
import metagn.kismet.vm.Memory

@CompileStatic
abstract class TypedTemplate implements KismetCallable, IKismetObject {
	abstract TypedExpression transform(TypedContext context, TypedExpression... args)

	IKismetObject call(Memory c, Expression... args) {
		throw new CannotOperateException("use runtime", "typedContext template")
	}

	TypedTemplate inner() { this }
}

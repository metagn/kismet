package metagn.kismet.call

import groovy.transform.CompileStatic
import metagn.kismet.lib.Functions
import metagn.kismet.parser.Parser
import metagn.kismet.scope.TypedContext
import metagn.kismet.type.Type
import metagn.kismet.type.TypeBound
import metagn.kismet.vm.IKismetObject
import metagn.kismet.vm.Memory

/*

 */

@CompileStatic
class KismetIterator {
	Expression inner

	IKismetObject iterate(Memory c, Expression toCall) {
		c.set('yield', new YieldTemplate(toCall))
		inner.evaluate(c)
	}

	TypedExpression generate(TypedContext tc, Expression toCall, Type preferred) {
		tc.addVariable('yield', new YieldTemplate(toCall), Functions.TEMPLATE_TYPE)
		inner.type(tc, new TypeBound(preferred))
	}

	static class YieldTemplate extends Template {
		Expression toCall

		YieldTemplate(Expression toCall) {
			this.toCall = toCall
		}

		Expression transform(Parser parser, Expression... args) {
			new ColonExpression(toCall, args[0])
		}
	}
}



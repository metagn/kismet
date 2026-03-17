package metagn.kismet.call

import groovy.transform.CompileStatic
import metagn.kismet.parser.Parser
import metagn.kismet.vm.IKismetObject
import metagn.kismet.vm.Memory

@CompileStatic
abstract class Template implements KismetCallable, IKismetObject {
	// doesn't transform arguments if true
	boolean isImmediate() { true }
	// doesn't transform result if true
	boolean isOptimized() { false }

	abstract Expression transform(Parser parser, Expression... args)

	IKismetObject call(Memory c, Expression... args) {
		transform(null, args).evaluate(c)
	}

	Template inner() { this }
}

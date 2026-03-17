package metagn.kismet.call

import groovy.transform.CompileStatic
import metagn.kismet.type.Type
import metagn.kismet.vm.IKismetObject
import metagn.kismet.vm.Memory

@CompileStatic
abstract class Instructor implements IKismetObject {
	Type argumentTypes
	Type returnType

	abstract IKismetObject call(Memory m, Instruction... args)

	Instructor inner() { this }
}


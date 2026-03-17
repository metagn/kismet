package metagn.kismet.call

import groovy.transform.CompileStatic
import metagn.kismet.vm.IKismetObject
import metagn.kismet.vm.Memory

@CompileStatic
interface KismetCallable {
	IKismetObject call(Memory c, Expression... args)
}

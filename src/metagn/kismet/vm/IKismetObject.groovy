package metagn.kismet.vm

import groovy.transform.CompileStatic

@CompileStatic
interface IKismetObject<T> {
	T inner()
}

package metagn.kismet.type

import groovy.transform.CompileStatic
import metagn.kismet.vm.IKismetObject

@CompileStatic
@Singleton(property = 'INSTANCE')
class AnyType extends AbstractType {
	TypeRelation weakRelation(Type other) {
		other == this ? TypeRelation.equal() : TypeRelation.supertype(Integer.MAX_VALUE)
	}

	String toString() { 'Any' }

	boolean check(IKismetObject object) { true }
}

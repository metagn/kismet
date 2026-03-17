package metagn.kismet.scope

import metagn.kismet.vm.IKismetObject

interface Address {
	String getName()
	IKismetObject getValue()
	void setValue(IKismetObject newValue)
}

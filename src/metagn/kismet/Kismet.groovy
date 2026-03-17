package metagn.kismet

import groovy.transform.CompileStatic
import metagn.kismet.lib.Prelude
import metagn.kismet.scope.Context
import metagn.kismet.vm.IKismetObject
import metagn.kismet.vm.KismetModels

@CompileStatic
class Kismet {
	static final IKismetObject NULL = KismetModels.KISMET_NULL
	static Prelude PRELUDE = new Prelude()
	static Context DEFAULT_CONTEXT = PRELUDE.defaultContext

	static IKismetObject model(x) {
		KismetModels.model(x)
	}
}

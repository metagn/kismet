package metagn.kismet.scope

import groovy.transform.CompileStatic
import metagn.kismet.vm.Memory

@CompileStatic
abstract class Module {
    abstract TypedContext typeContext()
    abstract Context getDefaultContext()
    abstract Memory run()
}

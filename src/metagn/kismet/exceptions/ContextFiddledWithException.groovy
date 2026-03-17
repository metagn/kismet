package metagn.kismet.exceptions

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors

@InheritConstructors
@CompileStatic
class ContextFiddledWithException extends KismetException {}

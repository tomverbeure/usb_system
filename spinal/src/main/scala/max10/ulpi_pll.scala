

package max10

import spinal.core._

class ulpi_pll() extends BlackBox{

    val inclk0          = in(Bool)
    val c0              = out(Bool)
    val locked          = out(Bool)

    setDefinitionName("ulpi_pll")
}




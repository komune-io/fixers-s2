package s2.bdd

import org.springframework.beans.factory.annotation.Autowired
import s2.bdd.data.TestContext

open class S2CucumberStepsDefinition: CucumberStepsDefinition() {
    // Spring injects it into a step class that every consumer subclasses with no constructor argument.
    @Suppress("LateinitUsage")
    @Autowired
    override lateinit var context: TestContext
}

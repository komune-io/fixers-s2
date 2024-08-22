package s2.sample.did.domain.client

import f2.client.ktor.F2ClientBuilder
import f2.client.ktor.Protocol
import f2.client.ktor.get
import f2.dsl.fnc.F2Supplier
import f2.dsl.fnc.F2SupplierSingle
import kotlin.js.Promise
import kotlinx.coroutines.asDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@JsName("didClient")
@JsExport
actual fun didClient(protocol: Protocol, host: String, port: Int, path: String?): F2SupplierSingle<DIDFunctionClient> {
    return F2SupplierSingle {
       val s2Client = F2ClientBuilder.get(protocol, host, port, path)
        DIDFunctionClient(s2Client)
    }
}

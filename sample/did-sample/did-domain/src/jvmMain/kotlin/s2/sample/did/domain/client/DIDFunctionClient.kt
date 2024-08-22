package s2.sample.did.domain.client

import f2.client.ktor.F2ClientBuilder
import f2.client.ktor.Protocol
import f2.client.ktor.get
import f2.dsl.fnc.F2Supplier
import f2.dsl.fnc.F2SupplierSingle
import kotlinx.coroutines.flow.flow

actual fun didClient(
    protocol: Protocol,
    host: String,
    port: Int,
    path: String?,
): F2SupplierSingle<DIDFunctionClient> {
    return F2SupplierSingle {
        F2ClientBuilder.get(protocol, host, port, path).let { s2Client ->
            DIDFunctionClient(s2Client)
        }
    }
}

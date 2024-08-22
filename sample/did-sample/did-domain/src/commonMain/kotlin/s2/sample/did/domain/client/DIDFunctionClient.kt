package s2.sample.did.domain.client

import f2.client.F2Client
import f2.client.function
import f2.client.ktor.Protocol
import f2.dsl.fnc.F2Supplier
import f2.dsl.fnc.F2SupplierSingle
import kotlin.js.JsExport
import kotlin.js.JsName
import s2.sample.did.domain.DidAggregate
import s2.sample.did.domain.features.DidAddPublicKeyCommandFunction
import s2.sample.did.domain.features.DidCreateCommandFunction
import s2.sample.did.domain.features.DidRevokeCommandFunction
import s2.sample.did.domain.features.DidRevokePublicKeyCommandFunction

expect fun didClient(
	protocol: Protocol,
	host: String,
	port: Int,
	path: String? = null
): F2SupplierSingle<DIDFunctionClient>

@JsName("DIDFunctionClient")
@JsExport
open class DIDFunctionClient constructor(private val client: F2Client) : DidAggregate {
	override fun createDid(): DidCreateCommandFunction = client.function("createDid")
	override fun addPublicKey(): DidAddPublicKeyCommandFunction = client.function("addPublicKey")
	override fun revokePublicKey(): DidRevokeCommandFunction = client.function("revokePublicKey")
	override fun revoke(): DidRevokePublicKeyCommandFunction = client.function("revoke")
}

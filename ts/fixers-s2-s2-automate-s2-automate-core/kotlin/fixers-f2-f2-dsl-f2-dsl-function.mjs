import {
  initMetadataForInterface1egvbzx539z91 as initMetadataForInterface,
  VOID3gxj6tk5isa35 as VOID,
  initMetadataForCompanion1wyw17z38v6ac as initMetadataForCompanion,
  protoOf180f3jzyo7rfj as protoOf,
  initMetadataForObject1cxne3s9w65el as initMetadataForObject,
  Unit_instance3kz1zqli6gr1r as Unit_instance,
  createThis2j2avj17cvnv2 as createThis,
  initMetadataForClassbxx6q50dy2s7 as initMetadataForClass,
  promisify1z0ncraq1ipzh as promisify,
  asList2ho2pewtsfvv as asList,
  copyToArray2j022khrow2yi as copyToArray,
} from './kotlin-kotlin-stdlib.mjs';
import {
  PluginGeneratedSerialDescriptorqdzeg5asqhfg as PluginGeneratedSerialDescriptor,
  IntSerializer_getInstance3jyhns9ue706g as IntSerializer_getInstance,
  typeParametersSerializers2likxjr48tr7y as typeParametersSerializers,
  GeneratedSerializer1f7t7hssdd2ws as GeneratedSerializer,
  throwMissingFieldException2cmke0v3ynf14 as throwMissingFieldException,
  SerializableWithd2dap36updxd as SerializableWith,
} from './kotlinx-serialization-kotlinx-serialization-core.mjs';
import {
  get_DEFAULT_CONCURRENCY5xfiedl9mtdj as get_DEFAULT_CONCURRENCY,
  asFlow3ngsnn5xpz8pw as asFlow,
  toList34jp97o2ftj8p as toList,
} from './kotlinx-coroutines-core.mjs';
//region block: imports
var imul = Math.imul;
//endregion
//region block: pre-declaration
class F2Consumer {}
class F2Function {}
class F2Supplier {}
class F2SupplierSingle {}
class Companion {}
class $serializer {
  constructor() {
    $serializer_instance = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('f2.dsl.fnc.operators.Batch', this, 2);
    tmp0_serialDesc.ii('size', true);
    tmp0_serialDesc.ii('concurrency', true);
    this.ol_1 = tmp0_serialDesc;
  }
  bf() {
    return this.ol_1;
  }
  fi() {
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [IntSerializer_getInstance(), IntSerializer_getInstance()];
  }
}
class Batch {
  constructor(size, concurrency) {
    size = size === VOID ? 128 : size;
    concurrency = concurrency === VOID ? get_BATCH_DEFAULT_CONCURRENCY() : concurrency;
    this.size = size;
    this.concurrency = concurrency;
  }
  n() {
    return this.size;
  }
  pl() {
    return this.concurrency;
  }
  jd() {
    return this.size;
  }
  kd() {
    return this.concurrency;
  }
  ql(size, concurrency) {
    return new Batch(size, concurrency);
  }
  copy(size, concurrency, $super) {
    size = size === VOID ? this.size : size;
    concurrency = concurrency === VOID ? this.concurrency : concurrency;
    return $super === VOID ? this.ql(size, concurrency) : $super.ql.call(this, size, concurrency);
  }
  toString() {
    return 'Batch(size=' + this.size + ', concurrency=' + this.concurrency + ')';
  }
  hashCode() {
    var result = this.size;
    result = imul(result, 31) + this.concurrency | 0;
    return result;
  }
  equals(other) {
    if (this === other)
      return true;
    if (!(other instanceof Batch))
      return false;
    if (!(this.size === other.size))
      return false;
    if (!(this.concurrency === other.concurrency))
      return false;
    return true;
  }
  static rl(seen0, size, concurrency, serializationConstructorMarker) {
    if (!(0 === (0 & seen0))) {
      throwMissingFieldException(seen0, 0, $serializer_getInstance().ol_1);
    }
    var $this = createThis(this);
    if (0 === (seen0 & 1))
      $this.size = 128;
    else
      $this.size = size;
    if (0 === (seen0 & 2))
      $this.concurrency = get_BATCH_DEFAULT_CONCURRENCY();
    else
      $this.concurrency = concurrency;
    return $this;
  }
}
//endregion
function get_BATCH_DEFAULT_CONCURRENCY() {
  _init_properties_batch_kt__hj63o8();
  return BATCH_DEFAULT_CONCURRENCY;
}
var BATCH_DEFAULT_CONCURRENCY;
var Companion_instance;
function Companion_getInstance() {
  return Companion_instance;
}
var $serializer_instance;
function $serializer_getInstance() {
  if ($serializer_instance === VOID)
    new $serializer();
  return $serializer_instance;
}
var properties_initialized_batch_kt_7asg5m;
function _init_properties_batch_kt__hj63o8() {
  if (!properties_initialized_batch_kt_7asg5m) {
    properties_initialized_batch_kt_7asg5m = true;
    BATCH_DEFAULT_CONCURRENCY = get_DEFAULT_CONCURRENCY();
  }
}
function invokeF2Function(fnc, msgs) {
  return promisify(($completion) => invokeF2Function_0(fnc, msgs, $completion));
}
function *invokeF2Function_0(fnc, msgs, $completion) {
  // Inline function 'kotlin.collections.toTypedArray' call
  var this_0 = yield* toList(yield* fnc(asFlow(asList(msgs)), $completion), VOID, $completion);
  return copyToArray(this_0);
}
function invokeF2Consumer(fnc, msgs) {
  return promisify(($completion) => invokeF2Consumer_0(fnc, msgs, $completion));
}
function *invokeF2Consumer_0(fnc, msgs, $completion) {
  yield* fnc(asFlow(asList(msgs)), $completion);
  return Unit_instance;
}
function invokeF2Supplier(fnc) {
  return promisify(($completion) => invokeF2Supplier_0(fnc, $completion));
}
function *invokeF2Supplier_0(fnc, $completion) {
  // Inline function 'kotlin.collections.toTypedArray' call
  var this_0 = yield* toList(yield* fnc($completion), VOID, $completion);
  return copyToArray(this_0);
}
function invokeF2SupplierSingle(fnc) {
  return promisify(($completion) => invokeF2SupplierSingle_0(fnc, $completion));
}
function *invokeF2SupplierSingle_0(fnc, $completion) {
  return yield* fnc($completion);
}
//region block: post-declaration
initMetadataForInterface(F2Consumer, 'F2Consumer', VOID, VOID, VOID, [1]);
initMetadataForInterface(F2Function, 'F2Function', VOID, VOID, VOID, [1]);
initMetadataForInterface(F2Supplier, 'F2Supplier', VOID, VOID, VOID, [0]);
initMetadataForInterface(F2SupplierSingle, 'F2SupplierSingle', VOID, VOID, VOID, [0]);
initMetadataForCompanion(Companion);
protoOf($serializer).gi = typeParametersSerializers;
initMetadataForObject($serializer, '$serializer', VOID, VOID, [GeneratedSerializer]);
initMetadataForClass(Batch, 'Batch', Batch, VOID, VOID, VOID, VOID, {0: $serializer_getInstance});
//endregion
//region block: init
Companion_instance = new Companion();
//endregion
//region block: exports
export {
  Batch as Batch,
  invokeF2Function as invokeF2Function,
  invokeF2Consumer as invokeF2Consumer,
  invokeF2Supplier as invokeF2Supplier,
  invokeF2SupplierSingle as invokeF2SupplierSingle,
};
//endregion

//# sourceMappingURL=fixers-f2-f2-dsl-f2-dsl-function.mjs.map

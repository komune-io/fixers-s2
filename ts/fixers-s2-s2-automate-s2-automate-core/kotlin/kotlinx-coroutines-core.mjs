import {
  Unit_instance3kz1zqli6gr1r as Unit_instance,
  initMetadataForClassbxx6q50dy2s7 as initMetadataForClass,
  VOID3gxj6tk5isa35 as VOID,
  initMetadataForInterface1egvbzx539z91 as initMetadataForInterface,
  ArrayList3it5z8td81qkl as ArrayList,
  $3l5vvh8yg0ier as $,
  equals2au1ep9vhcato as equals,
  FunctionAdapter3lcrrz3moet5b as FunctionAdapter,
  isInterface3d6p8outrmvmk as isInterface,
  hashCodeq5arwsb9dgti as hashCode,
  orPromise1iucc2to2udt2 as orPromise,
  constructCallableReference23y65rf941mch as constructCallableReference,
  fromInt2ii0rejb1w62w as fromInt,
  convertToInty04h231mmjoh as convertToInt,
  toLongOrNullutqivezb0wx1 as toLongOrNull,
  IllegalStateExceptionkoljg5n0nrlr as IllegalStateException,
  toString1pkumu07cwy4m as toString,
} from './kotlin-kotlin-stdlib.mjs';
//region block: imports
//endregion
//region block: pre-declaration
class asFlow$$inlined$unsafeFlow$1 {
  constructor($this_asFlow) {
    this.gl_1 = $this_asFlow;
  }
  *hl(collector, $completion) {
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s = this.gl_1.k();
    while (_iterator__ex2g4s.l()) {
      var element = _iterator__ex2g4s.m();
      yield* collector.il(element, $completion);
    }
    return Unit_instance;
  }
  jl(collector, $completion) {
    return this.hl(collector, $completion);
  }
}
class FlowCollector {}
class sam$kotlinx_coroutines_flow_FlowCollector$0 {
  constructor(function_0) {
    this.kl_1 = function_0;
  }
  *il(value, $completion) {
    return yield* $(this.kl_1(value, $completion), $completion);
  }
  w1() {
    return this.kl_1;
  }
  equals(other) {
    var tmp;
    if (!(other == null) ? isInterface(other, FlowCollector) : false) {
      var tmp_0;
      if (!(other == null) ? isInterface(other, FunctionAdapter) : false) {
        tmp_0 = equals(this.w1(), other.w1());
      } else {
        tmp_0 = false;
      }
      tmp = tmp_0;
    } else {
      tmp = false;
    }
    return tmp;
  }
  hashCode() {
    return hashCode(this.w1());
  }
}
//endregion
function asFlow(_this__u8e3s4) {
  // Inline function 'kotlinx.coroutines.flow.internal.unsafeFlow' call
  return new asFlow$$inlined$unsafeFlow$1(_this__u8e3s4);
}
function get_DEFAULT_CONCURRENCY() {
  _init_properties_Merge_kt__cbpr96();
  return DEFAULT_CONCURRENCY;
}
var DEFAULT_CONCURRENCY;
var properties_initialized_Merge_kt_dhn6vs;
function _init_properties_Merge_kt__cbpr96() {
  if (!properties_initialized_Merge_kt_dhn6vs) {
    properties_initialized_Merge_kt_dhn6vs = true;
    DEFAULT_CONCURRENCY = systemProp('kotlinx.coroutines.flow.defaultConcurrency', 16, 1, 2147483647);
  }
}
function *toList(_this__u8e3s4, destination, $completion) {
  destination = destination === VOID ? ArrayList.i() : destination;
  return yield* toCollection(_this__u8e3s4, destination, $completion);
}
function *toCollection(_this__u8e3s4, destination, $completion) {
  var tmp = toCollection$slambda(destination);
  yield* _this__u8e3s4.jl(new sam$kotlinx_coroutines_flow_FlowCollector$0(tmp), $completion);
  return destination;
}
function toCollection$slambda($destination) {
  return constructCallableReference((value, $completion) => orPromise($completion, function *($completion) {
    $destination.j(value);
    return Unit_instance;
  }), 1);
}
function systemProp(propertyName, defaultValue, minValue, maxValue) {
  minValue = minValue === VOID ? 1 : minValue;
  maxValue = maxValue === VOID ? 2147483647 : maxValue;
  return convertToInt(systemProp_0(propertyName, fromInt(defaultValue), fromInt(minValue), fromInt(maxValue)));
}
function systemProp_0(propertyName, defaultValue, minValue, maxValue) {
  minValue = minValue === VOID ? 1n : minValue;
  maxValue = maxValue === VOID ? 9223372036854775807n : maxValue;
  var tmp0_elvis_lhs = systemProp_1(propertyName);
  var tmp;
  if (tmp0_elvis_lhs == null) {
    return defaultValue;
  } else {
    tmp = tmp0_elvis_lhs;
  }
  var value = tmp;
  var tmp1_elvis_lhs = toLongOrNull(value);
  var tmp_0;
  if (tmp1_elvis_lhs == null) {
    // Inline function 'kotlin.error' call
    var message = "System property '" + propertyName + "' has unrecognized value '" + value + "'";
    throw IllegalStateException.u2(toString(message));
  } else {
    tmp_0 = tmp1_elvis_lhs;
  }
  var parsed = tmp_0;
  if (!(minValue <= parsed ? parsed <= maxValue : false)) {
    // Inline function 'kotlin.error' call
    var message_0 = "System property '" + propertyName + "' should be in range " + minValue.toString() + '..' + maxValue.toString() + ", but is '" + parsed.toString() + "'";
    throw IllegalStateException.u2(toString(message_0));
  }
  return parsed;
}
function systemProp_1(propertyName) {
  return null;
}
//region block: post-declaration
initMetadataForClass(asFlow$$inlined$unsafeFlow$1, VOID, VOID, VOID, VOID, [1]);
initMetadataForInterface(FlowCollector, 'FlowCollector', VOID, VOID, VOID, [1]);
initMetadataForClass(sam$kotlinx_coroutines_flow_FlowCollector$0, 'sam$kotlinx_coroutines_flow_FlowCollector$0', VOID, VOID, [FlowCollector, FunctionAdapter], [1]);
//endregion
//region block: exports
export {
  toList as toList34jp97o2ftj8p,
  get_DEFAULT_CONCURRENCY as get_DEFAULT_CONCURRENCY5xfiedl9mtdj,
  asFlow as asFlow3ngsnn5xpz8pw,
};
//endregion

//# sourceMappingURL=kotlinx-coroutines-core.mjs.map

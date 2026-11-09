import {
  initMetadataForInterface1egvbzx539z91 as initMetadataForInterface,
  Unit_instance3kz1zqli6gr1r as Unit_instance,
  THROW_IAE23kobfj9wdoxr as THROW_IAE,
  Enum3alwj03lh1n41 as Enum,
  initMetadataForClassbxx6q50dy2s7 as initMetadataForClass,
  defineProp3ur6h3slcvq4x as defineProp,
  VOID3gxj6tk5isa35 as VOID,
  getKClass3t8tygqu4lcxf as getKClass,
  getKClassFromExpression348iqjl4fnx2f as getKClassFromExpression,
  LazyThreadSafetyMode_PUBLICATION_getInstance23f213579at67 as LazyThreadSafetyMode_PUBLICATION_getInstance,
  lazy1261dae0bgscp as lazy,
  initMetadataForCompanion1wyw17z38v6ac as initMetadataForCompanion,
  protoOf180f3jzyo7rfj as protoOf,
  initMetadataForObject1cxne3s9w65el as initMetadataForObject,
  ArrayList3it5z8td81qkl as ArrayList,
  listOfNotNull2woi2boe01ub4 as listOfNotNull,
  addAll1k27qatfgp3k5 as addAll,
  copyToArray2j022khrow2yi as copyToArray,
  createThis2j2avj17cvnv2 as createThis,
  LinkedHashMap1zhqxkxv3xnkl as LinkedHashMap,
  collectionSizeOrDefault36dulx8yinfqm as collectionSizeOrDefault,
  distinct10qe1scfdvu5k as distinct,
  toString30pk9tzaqopn as toString,
  toString1pkumu07cwy4m as toString_0,
  IllegalArgumentException2asla15b5jaob as IllegalArgumentException,
} from './kotlin-kotlin-stdlib.mjs';
import {
  ReferenceArraySerializer3juj1vqolxkrs as ReferenceArraySerializer,
  PluginGeneratedSerialDescriptorqdzeg5asqhfg as PluginGeneratedSerialDescriptor,
  StringSerializer_getInstance1p9zda6r6kb23 as StringSerializer_getInstance,
  get_nullable197rfua9r7fsz as get_nullable,
  BooleanSerializer_getInstance22aja3hu17sxf as BooleanSerializer_getInstance,
  typeParametersSerializers2likxjr48tr7y as typeParametersSerializers,
  GeneratedSerializer1f7t7hssdd2ws as GeneratedSerializer,
  throwMissingFieldException2cmke0v3ynf14 as throwMissingFieldException,
  SerializableWithd2dap36updxd as SerializableWith,
  IntSerializer_getInstance3jyhns9ue706g as IntSerializer_getInstance,
} from './kotlinx-serialization-kotlinx-serialization-core.mjs';
import {
  Commandj5jg47cicltn as Command,
  Eventox8naz25uncx as Event,
} from './fixers-f2-f2-dsl-f2-dsl-cqrs.mjs';
//region block: imports
//endregion
//region block: pre-declaration
class Automate {}
class ErrorCategory extends Enum {
  get name() {
    return this.s1();
  }
  get ordinal() {
    return this.t1();
  }
}
class Companion {
  constructor() {
    Companion_instance = this;
    var tmp = this;
    var tmp_0 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    tmp.ul_1 = [null, null, lazy(tmp_0, S2Automate$Companion$$childSerializers$_anonymous__4k10sp), null];
  }
}
class $serializer {
  constructor() {
    $serializer_instance = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('s2.dsl.automate.S2Automate', this, 4);
    tmp0_serialDesc.ii('name', false);
    tmp0_serialDesc.ii('version', false);
    tmp0_serialDesc.ii('transitions', false);
    tmp0_serialDesc.ii('withResultAsAction', true);
    this.vl_1 = tmp0_serialDesc;
  }
  bf() {
    return this.vl_1;
  }
  fi() {
    var tmp0_cached = Companion_getInstance().ul_1;
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [StringSerializer_getInstance(), get_nullable(StringSerializer_getInstance()), tmp0_cached[2].i1(), BooleanSerializer_getInstance()];
  }
}
class S2Automate {
  constructor(name, version, transitions) {
    Companion_getInstance();
    this.name = name;
    this.version = version;
    this.transitions = transitions;
    var tmp = this;
    var tmp0 = this.transitions;
    var tmp$ret$0;
    $l$block: {
      // Inline function 'kotlin.collections.all' call
      var inductionVariable = 0;
      var last = tmp0.length;
      while (inductionVariable < last) {
        var element = tmp0[inductionVariable];
        inductionVariable = inductionVariable + 1 | 0;
        if (!!(element.result == null)) {
          tmp$ret$0 = false;
          break $l$block;
        }
      }
      tmp$ret$0 = true;
    }
    tmp.withResultAsAction = tmp$ret$0;
    // Inline function 'kotlin.collections.flatMap' call
    var tmp0_0 = this.transitions;
    // Inline function 'kotlin.collections.flatMapTo' call
    var destination = ArrayList.i();
    var inductionVariable_0 = 0;
    var last_0 = tmp0_0.length;
    while (inductionVariable_0 < last_0) {
      var element_0 = tmp0_0[inductionVariable_0];
      inductionVariable_0 = inductionVariable_0 + 1 | 0;
      var list = listOfNotNull([element_0.from, element_0.to]);
      addAll(destination, list);
    }
    var states = destination;
    var tmp_0 = S2Automate$lambda;
    requireOneToOne(states, tmp_0, S2Automate$lambda_0, 'name');
    var tmp_1 = S2Automate$lambda_1;
    requireOneToOne(states, tmp_1, S2Automate$lambda_2, 'position');
  }
  s1() {
    return this.name;
  }
  wl() {
    return this.version;
  }
  xl() {
    return this.transitions;
  }
  yl() {
    return this.withResultAsAction;
  }
  getAvailableTransitions(state) {
    // Inline function 'kotlin.collections.filter' call
    var tmp0 = this.transitions;
    // Inline function 'kotlin.collections.filterTo' call
    var destination = ArrayList.i();
    var inductionVariable = 0;
    var last = tmp0.length;
    while (inductionVariable < last) {
      var element = tmp0[inductionVariable];
      inductionVariable = inductionVariable + 1 | 0;
      if (isSame(this, element.from, state)) {
        destination.j(element);
      }
    }
    // Inline function 'kotlin.collections.toTypedArray' call
    return copyToArray(destination);
  }
  isAvailableTransition(currentState, msg) {
    var tmp0 = this.getAvailableTransitions(currentState);
    var tmp$ret$0;
    $l$block: {
      // Inline function 'kotlin.collections.any' call
      var inductionVariable = 0;
      var last = tmp0.length;
      while (inductionVariable < last) {
        var element = tmp0[inductionVariable];
        inductionVariable = inductionVariable + 1 | 0;
        if (isInstance(this, element.action, msg)) {
          tmp$ret$0 = true;
          break $l$block;
        }
      }
      tmp$ret$0 = false;
    }
    return tmp$ret$0;
  }
  isAvailableInitTransition(command) {
    var tmp0 = this.transitions;
    var tmp$ret$0;
    $l$block: {
      // Inline function 'kotlin.collections.any' call
      var inductionVariable = 0;
      var last = tmp0.length;
      while (inductionVariable < last) {
        var element = tmp0[inductionVariable];
        inductionVariable = inductionVariable + 1 | 0;
        if (element.from == null && isInstance(this, element.action, command)) {
          tmp$ret$0 = true;
          break $l$block;
        }
      }
      tmp$ret$0 = false;
    }
    return tmp$ret$0;
  }
  isFinalState(state) {
    // Inline function 'kotlin.collections.isEmpty' call
    return this.getAvailableTransitions(state).length === 0;
  }
  isSameState(from, to) {
    return (from == null ? null : from.position) === to.position;
  }
  static zl(seen0, name, version, transitions, withResultAsAction, serializationConstructorMarker) {
    Companion_getInstance();
    if (!(7 === (7 & seen0))) {
      throwMissingFieldException(seen0, 7, $serializer_getInstance().vl_1);
    }
    var $this = createThis(this);
    $this.name = name;
    $this.version = version;
    $this.transitions = transitions;
    if (0 === (seen0 & 8)) {
      var tmp = $this;
      var tmp0 = $this.transitions;
      var tmp$ret$0;
      $l$block: {
        // Inline function 'kotlin.collections.all' call
        var inductionVariable = 0;
        var last = tmp0.length;
        while (inductionVariable < last) {
          var element = tmp0[inductionVariable];
          inductionVariable = inductionVariable + 1 | 0;
          if (!!(element.result == null)) {
            tmp$ret$0 = false;
            break $l$block;
          }
        }
        tmp$ret$0 = true;
      }
      tmp.withResultAsAction = tmp$ret$0;
    } else
      $this.withResultAsAction = withResultAsAction;
    // Inline function 'kotlin.collections.flatMap' call
    var tmp0_0 = $this.transitions;
    // Inline function 'kotlin.collections.flatMapTo' call
    var destination = ArrayList.i();
    var inductionVariable_0 = 0;
    var last_0 = tmp0_0.length;
    while (inductionVariable_0 < last_0) {
      var element_0 = tmp0_0[inductionVariable_0];
      inductionVariable_0 = inductionVariable_0 + 1 | 0;
      var list = listOfNotNull([element_0.from, element_0.to]);
      addAll(destination, list);
    }
    var states = destination;
    var tmp_0 = S2Automate$_init_$lambda_mnzswo;
    requireOneToOne(states, tmp_0, S2Automate$_init_$lambda_mnzswo_0, 'name');
    var tmp_1 = S2Automate$_init_$lambda_mnzswo_1;
    requireOneToOne(states, tmp_1, S2Automate$_init_$lambda_mnzswo_2, 'position');
    return $this;
  }
}
class S2InitCommand {}
class WithId {}
class S2Command {}
class S2Error {}
class S2ErrorBase {
  constructor(type, description, date, payload, cause) {
    cause = cause === VOID ? null : cause;
    this.dm_1 = type;
    this.em_1 = description;
    this.fm_1 = date;
    this.gm_1 = payload;
    this.hm_1 = cause;
  }
  qi() {
    return this.dm_1;
  }
  am() {
    return this.em_1;
  }
  bm() {
    return this.fm_1;
  }
  cm() {
    return this.gm_1;
  }
  aa() {
    return this.hm_1;
  }
  toString() {
    return "S2ErrorBase(type='" + this.dm_1 + "', description='" + this.em_1 + "', date='" + this.fm_1 + "', payload=" + toString_0(this.gm_1) + ')';
  }
  get type() {
    return this.qi();
  }
  get description() {
    return this.am();
  }
  get date() {
    return this.bm();
  }
  get payload() {
    return this.cm();
  }
  get cause() {
    return this.aa();
  }
}
class S2Event {}
class S2EventSuccess {
  constructor(id, type, from, to) {
    this.id = id;
    this.type = type;
    this.from = from;
    this.to = to;
  }
  oi() {
    return this.id;
  }
  qi() {
    return this.type;
  }
  im() {
    return this.from;
  }
  jm() {
    return this.to;
  }
}
class S2EventError {
  constructor(id, type, from, to, error) {
    this.id = id;
    this.type = type;
    this.from = from;
    this.to = to;
    this.error = error;
  }
  oi() {
    return this.id;
  }
  qi() {
    return this.type;
  }
  im() {
    return this.from;
  }
  jm() {
    return this.to;
  }
  tj() {
    return this.error;
  }
}
class S2Role {}
class S2State {}
class S2InitTransition {
  constructor(to, role, action) {
    this.lm_1 = to;
    this.mm_1 = role;
    this.nm_1 = action;
  }
  jm() {
    return this.lm_1;
  }
  om() {
    return this.mm_1;
  }
  pm() {
    return this.nm_1;
  }
  get to() {
    return this.jm();
  }
  get role() {
    return this.om();
  }
  get action() {
    return this.pm();
  }
}
class Companion_0 {}
class $serializer_0 {
  constructor() {
    $serializer_instance_0 = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('s2.dsl.automate.S2Transition', this, 5);
    tmp0_serialDesc.ii('from', false);
    tmp0_serialDesc.ii('to', false);
    tmp0_serialDesc.ii('role', false);
    tmp0_serialDesc.ii('action', false);
    tmp0_serialDesc.ii('result', false);
    this.qm_1 = tmp0_serialDesc;
  }
  bf() {
    return this.qm_1;
  }
  fi() {
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [get_nullable($serializer_getInstance_3()), $serializer_getInstance_3(), $serializer_getInstance_2(), $serializer_getInstance_1(), get_nullable($serializer_getInstance_1())];
  }
}
class S2Transition {
  constructor(from, to, role, action, result) {
    this.rm_1 = from;
    this.sm_1 = to;
    this.tm_1 = role;
    this.um_1 = action;
    this.vm_1 = result;
  }
  im() {
    return this.rm_1;
  }
  jm() {
    return this.sm_1;
  }
  om() {
    return this.tm_1;
  }
  pm() {
    return this.um_1;
  }
  wm() {
    return this.vm_1;
  }
  static xm(seen0, from, to, role, action, result, serializationConstructorMarker) {
    if (!(31 === (31 & seen0))) {
      throwMissingFieldException(seen0, 31, $serializer_getInstance_0().qm_1);
    }
    var $this = createThis(this);
    $this.rm_1 = from;
    $this.sm_1 = to;
    $this.tm_1 = role;
    $this.um_1 = action;
    $this.vm_1 = result;
    return $this;
  }
  get from() {
    return this.im();
  }
  get to() {
    return this.jm();
  }
  get role() {
    return this.om();
  }
  get action() {
    return this.pm();
  }
  get result() {
    return this.wm();
  }
}
class Companion_1 {}
class $serializer_1 {
  constructor() {
    $serializer_instance_1 = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('s2.dsl.automate.S2TransitionValue', this, 1);
    tmp0_serialDesc.ii('name', false);
    this.ym_1 = tmp0_serialDesc;
  }
  bf() {
    return this.ym_1;
  }
  fi() {
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [StringSerializer_getInstance()];
  }
}
class S2TransitionValue {
  constructor(name) {
    this.name = name;
  }
  s1() {
    return this.name;
  }
  static zm(seen0, name, serializationConstructorMarker) {
    if (!(1 === (1 & seen0))) {
      throwMissingFieldException(seen0, 1, $serializer_getInstance_1().ym_1);
    }
    var $this = createThis(this);
    $this.name = name;
    return $this;
  }
}
class Companion_2 {}
class $serializer_2 {
  constructor() {
    $serializer_instance_2 = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('s2.dsl.automate.S2RoleValue', this, 1);
    tmp0_serialDesc.ii('name', false);
    this.an_1 = tmp0_serialDesc;
  }
  bf() {
    return this.an_1;
  }
  fi() {
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [StringSerializer_getInstance()];
  }
}
class S2RoleValue {
  constructor(name) {
    this.name = name;
  }
  s1() {
    return this.name;
  }
  static bn(seen0, name, serializationConstructorMarker) {
    if (!(1 === (1 & seen0))) {
      throwMissingFieldException(seen0, 1, $serializer_getInstance_2().an_1);
    }
    var $this = createThis(this);
    $this.name = name;
    return $this;
  }
}
class Companion_3 {}
class $serializer_3 {
  constructor() {
    $serializer_instance_3 = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('s2.dsl.automate.S2StateValue', this, 2);
    tmp0_serialDesc.ii('name', false);
    tmp0_serialDesc.ii('position', false);
    this.cn_1 = tmp0_serialDesc;
  }
  bf() {
    return this.cn_1;
  }
  fi() {
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [StringSerializer_getInstance(), IntSerializer_getInstance()];
  }
}
class S2StateValue {
  constructor(name, position) {
    this.name = name;
    this.position = position;
  }
  s1() {
    return this.name;
  }
  km() {
    return this.position;
  }
  static dn(seen0, name, position, serializationConstructorMarker) {
    if (!(3 === (3 & seen0))) {
      throwMissingFieldException(seen0, 3, $serializer_getInstance_3().cn_1);
    }
    var $this = createThis(this);
    $this.name = name;
    $this.position = position;
    return $this;
  }
}
class WithS2Id {}
class WithS2State {}
class WithS2IdAndStatus {}
class WithS2Iteration {}
//endregion
var static_init_called;
function static_init() {
  if (static_init_called)
    return Unit_instance;
  static_init_called = true;
  ErrorCategory_Rejected_instance = new ErrorCategory('Rejected', 0);
  ErrorCategory_Transient_instance = new ErrorCategory('Transient', 1);
  ErrorCategory_Indeterminate_instance = new ErrorCategory('Indeterminate', 2);
  ErrorCategory_Conflict_instance = new ErrorCategory('Conflict', 3);
}
var ErrorCategory_Rejected_instance;
var ErrorCategory_Transient_instance;
var ErrorCategory_Indeterminate_instance;
var ErrorCategory_Conflict_instance;
function values() {
  static_init();
  return [ErrorCategory_Rejected_getInstance(), ErrorCategory_Transient_getInstance(), ErrorCategory_Indeterminate_getInstance(), ErrorCategory_Conflict_getInstance()];
}
function valueOf(value) {
  static_init();
  switch (value) {
    case 'Rejected':
      return ErrorCategory_Rejected_getInstance();
    case 'Transient':
      return ErrorCategory_Transient_getInstance();
    case 'Indeterminate':
      return ErrorCategory_Indeterminate_getInstance();
    case 'Conflict':
      return ErrorCategory_Conflict_getInstance();
    default:
      THROW_IAE('No enum constant s2.dsl.automate.ErrorCategory.' + value);
      break;
  }
}
function ErrorCategory_Rejected_getInstance() {
  static_init();
  return ErrorCategory_Rejected_instance;
}
function ErrorCategory_Transient_getInstance() {
  static_init();
  return ErrorCategory_Transient_instance;
}
function ErrorCategory_Indeterminate_getInstance() {
  static_init();
  return ErrorCategory_Indeterminate_instance;
}
function ErrorCategory_Conflict_getInstance() {
  static_init();
  return ErrorCategory_Conflict_instance;
}
function S2Automate$Companion$$childSerializers$_anonymous__4k10sp() {
  return new ReferenceArraySerializer(getKClass(S2Transition), $serializer_getInstance_0());
}
function isInstance($this, $receiver, msg) {
  var msgAction = toValue(getKClassFromExpression(msg));
  return msgAction.name === $receiver.name;
}
function isSame($this, $receiver, to) {
  return ($receiver == null ? null : $receiver.position) === to.position;
}
var Companion_instance;
function Companion_getInstance() {
  if (Companion_instance === VOID)
    new Companion();
  return Companion_instance;
}
var $serializer_instance;
function $serializer_getInstance() {
  if ($serializer_instance === VOID)
    new $serializer();
  return $serializer_instance;
}
function S2Automate$lambda(it) {
  return it.name;
}
function S2Automate$lambda_0(it) {
  return it.position;
}
function S2Automate$lambda_1(it) {
  return it.position;
}
function S2Automate$lambda_2(it) {
  return it.name;
}
function S2Automate$_init_$lambda_mnzswo(it) {
  return it.name;
}
function S2Automate$_init_$lambda_mnzswo_0(it) {
  return it.position;
}
function S2Automate$_init_$lambda_mnzswo_1(it) {
  return it.position;
}
function S2Automate$_init_$lambda_mnzswo_2(it) {
  return it.name;
}
function requireOneToOne(states, key, value, label) {
  // Inline function 'kotlin.collections.groupBy' call
  // Inline function 'kotlin.collections.groupByTo' call
  var destination = LinkedHashMap.l2();
  var _iterator__ex2g4s = states.k();
  while (_iterator__ex2g4s.l()) {
    var element = _iterator__ex2g4s.m();
    var key_0 = key(element);
    // Inline function 'kotlin.collections.getOrPut' call
    var value_0 = destination.m1(key_0);
    var tmp;
    if (value_0 == null) {
      var answer = ArrayList.i();
      destination.q2(key_0, answer);
      tmp = answer;
    } else {
      tmp = value_0;
    }
    var list = tmp;
    list.j(element);
  }
  // Inline function 'kotlin.collections.forEach' call
  // Inline function 'kotlin.collections.iterator' call
  var _iterator__ex2g4s_0 = destination.p1().k();
  while (_iterator__ex2g4s_0.l()) {
    var element_0 = _iterator__ex2g4s_0.m();
    // Inline function 'kotlin.collections.component1' call
    var k = element_0.h1();
    // Inline function 'kotlin.collections.component2' call
    var sharing = element_0.i1();
    // Inline function 'kotlin.collections.map' call
    // Inline function 'kotlin.collections.mapTo' call
    var destination_0 = ArrayList.o(collectionSizeOrDefault(sharing, 10));
    var _iterator__ex2g4s_1 = sharing.k();
    while (_iterator__ex2g4s_1.l()) {
      var item = _iterator__ex2g4s_1.m();
      destination_0.j(value(item));
    }
    var values = distinct(destination_0);
    // Inline function 'kotlin.require' call
    if (!(values.n() === 1)) {
      var message = 'States sharing the ' + label + ' ' + toString(k) + ': ' + toString_0(values);
      throw IllegalArgumentException.g3(toString_0(message));
    }
  }
}
var Companion_instance_0;
function Companion_getInstance_0() {
  return Companion_instance_0;
}
var $serializer_instance_0;
function $serializer_getInstance_0() {
  if ($serializer_instance_0 === VOID)
    new $serializer_0();
  return $serializer_instance_0;
}
var Companion_instance_1;
function Companion_getInstance_1() {
  return Companion_instance_1;
}
var $serializer_instance_1;
function $serializer_getInstance_1() {
  if ($serializer_instance_1 === VOID)
    new $serializer_1();
  return $serializer_instance_1;
}
var Companion_instance_2;
function Companion_getInstance_2() {
  return Companion_instance_2;
}
var $serializer_instance_2;
function $serializer_getInstance_2() {
  if ($serializer_instance_2 === VOID)
    new $serializer_2();
  return $serializer_instance_2;
}
var Companion_instance_3;
function Companion_getInstance_3() {
  return Companion_instance_3;
}
var $serializer_instance_3;
function $serializer_getInstance_3() {
  if ($serializer_instance_3 === VOID)
    new $serializer_3();
  return $serializer_instance_3;
}
function toValue(_this__u8e3s4) {
  return new S2TransitionValue(s2SimpleName(_this__u8e3s4));
}
function s2SimpleName(_this__u8e3s4) {
  var tmp0 = _this__u8e3s4.wa();
  var tmp$ret$0;
  $l$block: {
    // Inline function 'kotlin.requireNotNull' call
    if (tmp0 == null) {
      var message = 'An S2 class needs a name: ' + toString_0(_this__u8e3s4);
      throw IllegalArgumentException.g3(toString_0(message));
    } else {
      tmp$ret$0 = tmp0;
      break $l$block;
    }
  }
  return tmp$ret$0;
}
//region block: post-declaration
initMetadataForInterface(Automate, 'Automate');
initMetadataForClass(ErrorCategory, 'ErrorCategory');
initMetadataForCompanion(Companion);
protoOf($serializer).gi = typeParametersSerializers;
initMetadataForObject($serializer, '$serializer', VOID, VOID, [GeneratedSerializer]);
initMetadataForClass(S2Automate, 'S2Automate', VOID, VOID, [Automate], VOID, VOID, {0: $serializer_getInstance});
initMetadataForInterface(S2InitCommand, 'S2InitCommand', VOID, VOID, [Command]);
initMetadataForInterface(WithId, 'WithId');
initMetadataForInterface(S2Command, 'S2Command', VOID, VOID, [Command, WithId]);
initMetadataForInterface(S2Error, 'S2Error');
initMetadataForClass(S2ErrorBase, 'S2ErrorBase', VOID, VOID, [S2Error]);
initMetadataForInterface(S2Event, 'S2Event', VOID, VOID, [Event, WithId]);
initMetadataForClass(S2EventSuccess, 'S2EventSuccess', VOID, VOID, [Event]);
initMetadataForClass(S2EventError, 'S2EventError', VOID, VOID, [Event]);
initMetadataForInterface(S2Role, 'S2Role');
initMetadataForInterface(S2State, 'S2State');
initMetadataForClass(S2InitTransition, 'S2InitTransition');
initMetadataForCompanion(Companion_0);
protoOf($serializer_0).gi = typeParametersSerializers;
initMetadataForObject($serializer_0, '$serializer', VOID, VOID, [GeneratedSerializer]);
initMetadataForClass(S2Transition, 'S2Transition', VOID, VOID, VOID, VOID, VOID, {0: $serializer_getInstance_0});
initMetadataForCompanion(Companion_1);
protoOf($serializer_1).gi = typeParametersSerializers;
initMetadataForObject($serializer_1, '$serializer', VOID, VOID, [GeneratedSerializer]);
initMetadataForClass(S2TransitionValue, 'S2TransitionValue', VOID, VOID, VOID, VOID, VOID, {0: $serializer_getInstance_1});
initMetadataForCompanion(Companion_2);
protoOf($serializer_2).gi = typeParametersSerializers;
initMetadataForObject($serializer_2, '$serializer', VOID, VOID, [GeneratedSerializer]);
initMetadataForClass(S2RoleValue, 'S2RoleValue', VOID, VOID, VOID, VOID, VOID, {0: $serializer_getInstance_2});
initMetadataForCompanion(Companion_3);
protoOf($serializer_3).gi = typeParametersSerializers;
initMetadataForObject($serializer_3, '$serializer', VOID, VOID, [GeneratedSerializer]);
initMetadataForClass(S2StateValue, 'S2StateValue', VOID, VOID, VOID, VOID, VOID, {0: $serializer_getInstance_3});
initMetadataForInterface(WithS2Id, 'WithS2Id');
initMetadataForInterface(WithS2State, 'WithS2State');
initMetadataForInterface(WithS2IdAndStatus, 'WithS2IdAndStatus', VOID, VOID, [WithS2Id, WithS2State]);
initMetadataForInterface(WithS2Iteration, 'WithS2Iteration');
//endregion
//region block: init
Companion_instance_0 = new Companion_0();
Companion_instance_1 = new Companion_1();
Companion_instance_2 = new Companion_2();
Companion_instance_3 = new Companion_3();
//endregion
//region block: exports
ErrorCategory.values = values;
ErrorCategory.valueOf = valueOf;
defineProp(ErrorCategory, 'Rejected', ErrorCategory_Rejected_getInstance, VOID, true);
defineProp(ErrorCategory, 'Transient', ErrorCategory_Transient_getInstance, VOID, true);
defineProp(ErrorCategory, 'Indeterminate', ErrorCategory_Indeterminate_getInstance, VOID, true);
defineProp(ErrorCategory, 'Conflict', ErrorCategory_Conflict_getInstance, VOID, true);
export {
  ErrorCategory as ErrorCategory,
  S2Automate as S2Automate,
  S2ErrorBase as S2ErrorBase,
  S2EventSuccess as S2EventSuccess,
  S2EventError as S2EventError,
  S2InitTransition as S2InitTransition,
  S2Transition as S2Transition,
  S2TransitionValue as S2TransitionValue,
  S2RoleValue as S2RoleValue,
  S2StateValue as S2StateValue,
};
//endregion

//# sourceMappingURL=fixers-s2-s2-automate-s2-automate-dsl.mjs.map

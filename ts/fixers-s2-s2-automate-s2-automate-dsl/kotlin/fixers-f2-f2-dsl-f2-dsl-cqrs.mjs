import {
  initMetadataForInterface1egvbzx539z91 as initMetadataForInterface,
  VOID3gxj6tk5isa35 as VOID,
  initMetadataForCompanion1wyw17z38v6ac as initMetadataForCompanion,
  Unit_instance3kz1zqli6gr1r as Unit_instance,
  createThis2j2avj17cvnv2 as createThis,
  initMetadataForClassbxx6q50dy2s7 as initMetadataForClass,
  protoOf180f3jzyo7rfj as protoOf,
  initMetadataForObject1cxne3s9w65el as initMetadataForObject,
  Companion_getInstance2o9uk4k3kfl5 as Companion_getInstance,
  System_instance34rnn1q53nv02 as System_instance,
  RuntimeException1r3t0zl97011n as RuntimeException,
  captureStack1fzi4aczwc4hg as captureStack,
  defineProp3ur6h3slcvq4x as defineProp,
  listOf1jh22dvmctj1r as listOf,
  ArrayList3it5z8td81qkl as ArrayList,
  collectionSizeOrDefault36dulx8yinfqm as collectionSizeOrDefault,
  plus20p0vtfmu0596 as plus,
  toString1pkumu07cwy4m as toString,
  hashCodeq5arwsb9dgti as hashCode,
  getBooleanHashCode1bbj3u6b3v0a7 as getBooleanHashCode,
  equals2au1ep9vhcato as equals,
  getKClass3t8tygqu4lcxf as getKClass,
  LazyThreadSafetyMode_PUBLICATION_getInstance23f213579at67 as LazyThreadSafetyMode_PUBLICATION_getInstance,
  lazy1261dae0bgscp as lazy,
} from './kotlin-kotlin-stdlib.mjs';
import {
  PluginGeneratedSerialDescriptorqdzeg5asqhfg as PluginGeneratedSerialDescriptor,
  throwMissingFieldException2cmke0v3ynf14 as throwMissingFieldException,
  SerializableWithd2dap36updxd as SerializableWith,
  StringSerializer_getInstance1p9zda6r6kb23 as StringSerializer_getInstance,
  get_nullable197rfua9r7fsz as get_nullable,
  IntSerializer_getInstance3jyhns9ue706g as IntSerializer_getInstance,
  typeParametersSerializers2likxjr48tr7y as typeParametersSerializers,
  GeneratedSerializer1f7t7hssdd2ws as GeneratedSerializer,
  PolymorphicSerializer3p3fzpdobi8hh as PolymorphicSerializer,
} from './kotlinx-serialization-kotlinx-serialization-core.mjs';
//region block: imports
var imul = Math.imul;
//endregion
//region block: pre-declaration
class Message {}
class Command {}
class Event {}
class Problem {}
class Query {}
class WithEnvelopeId {}
class WithEnvelopeData {}
class EnvelopeDTO {}
class Companion {
  constructor() {
    Companion_instance = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('f2.dsl.cqrs.envelope.Envelope', null, 7);
    tmp0_serialDesc.ch('id', false);
    tmp0_serialDesc.ch('data', false);
    tmp0_serialDesc.ch('type', false);
    tmp0_serialDesc.ch('datacontenttype', true);
    tmp0_serialDesc.ch('specversion', true);
    tmp0_serialDesc.ch('source', true);
    tmp0_serialDesc.ch('time', true);
    this.ph_1 = tmp0_serialDesc;
  }
}
class Envelope {
  constructor(id, data, type, datacontenttype, specversion, source, time) {
    Companion_getInstance_0();
    datacontenttype = datacontenttype === VOID ? null : datacontenttype;
    specversion = specversion === VOID ? null : specversion;
    source = source === VOID ? null : source;
    time = time === VOID ? null : time;
    this.qh_1 = id;
    this.rh_1 = data;
    this.sh_1 = type;
    this.th_1 = datacontenttype;
    this.uh_1 = specversion;
    this.vh_1 = source;
    this.wh_1 = time;
  }
  ih() {
    return this.qh_1;
  }
  jh() {
    return this.rh_1;
  }
  kh() {
    return this.sh_1;
  }
  lh() {
    return this.th_1;
  }
  mh() {
    return this.uh_1;
  }
  nh() {
    return this.vh_1;
  }
  oh() {
    return this.wh_1;
  }
  static xh(seen0, id, data, type, datacontenttype, specversion, source, time, serializationConstructorMarker) {
    Companion_getInstance_0();
    if (!(7 === (7 & seen0))) {
      throwMissingFieldException(seen0, 7, Companion_getInstance_0().ph_1);
    }
    var $this = createThis(this);
    $this.qh_1 = id;
    $this.rh_1 = data;
    $this.sh_1 = type;
    if (0 === (seen0 & 8))
      $this.th_1 = null;
    else
      $this.th_1 = datacontenttype;
    if (0 === (seen0 & 16))
      $this.uh_1 = null;
    else
      $this.uh_1 = specversion;
    if (0 === (seen0 & 32))
      $this.vh_1 = null;
    else
      $this.vh_1 = source;
    if (0 === (seen0 & 64))
      $this.wh_1 = null;
    else
      $this.wh_1 = time;
    return $this;
  }
  get id() {
    return this.ih();
  }
  get data() {
    return this.jh();
  }
  get type() {
    return this.kh();
  }
  get datacontenttype() {
    return this.lh();
  }
  get specversion() {
    return this.mh();
  }
  get source() {
    return this.nh();
  }
  get time() {
    return this.oh();
  }
}
class F2ErrorDTO {}
class Companion_0 {}
class $serializer {
  constructor() {
    $serializer_instance = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('f2.dsl.cqrs.error.F2Error', this, 5);
    tmp0_serialDesc.ch('message', false);
    tmp0_serialDesc.ch('id', true);
    tmp0_serialDesc.ch('timestamp', true);
    tmp0_serialDesc.ch('code', true);
    tmp0_serialDesc.ch('requestId', true);
    this.bi_1 = tmp0_serialDesc;
  }
  vd() {
    return this.bi_1;
  }
  zg() {
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [StringSerializer_getInstance(), get_nullable(StringSerializer_getInstance()), StringSerializer_getInstance(), IntSerializer_getInstance(), get_nullable(StringSerializer_getInstance())];
  }
}
class F2Error {
  constructor(message, id, timestamp, code, requestId) {
    id = id === VOID ? Companion_getInstance().pd().toString() : id;
    timestamp = timestamp === VOID ? System_instance.tc().toString() : timestamp;
    code = code === VOID ? 500 : code;
    requestId = requestId === VOID ? null : requestId;
    this.ci_1 = message;
    this.di_1 = id;
    this.ei_1 = timestamp;
    this.fi_1 = code;
    this.gi_1 = requestId;
  }
  x8() {
    return this.ci_1;
  }
  ih() {
    return this.di_1;
  }
  yh() {
    return this.ei_1;
  }
  zh() {
    return this.fi_1;
  }
  ai() {
    return this.gi_1;
  }
  toString() {
    return "F2Error(timestamp='" + this.timestamp + "', code=" + this.code + ", requestId='" + this.requestId + "', message='" + this.message + "')";
  }
  static hi(seen0, message, id, timestamp, code, requestId, serializationConstructorMarker) {
    if (!(1 === (1 & seen0))) {
      throwMissingFieldException(seen0, 1, $serializer_getInstance().bi_1);
    }
    var $this = createThis(this);
    $this.ci_1 = message;
    if (0 === (seen0 & 2))
      $this.di_1 = Companion_getInstance().pd().toString();
    else
      $this.di_1 = id;
    if (0 === (seen0 & 4))
      $this.ei_1 = System_instance.tc().toString();
    else
      $this.ei_1 = timestamp;
    if (0 === (seen0 & 8))
      $this.fi_1 = 500;
    else
      $this.fi_1 = code;
    if (0 === (seen0 & 16))
      $this.gi_1 = null;
    else
      $this.gi_1 = requestId;
    return $this;
  }
  get message() {
    return this.x8();
  }
  get id() {
    return this.ih();
  }
  get timestamp() {
    return this.yh();
  }
  get code() {
    return this.zh();
  }
  get requestId() {
    return this.ai();
  }
}
class Companion_1 {
  ii(message, id, requestId, code, cause) {
    var tmp0_timestamp = System_instance.tc().toString();
    return F2Exception.mi(new F2Error(message, id, tmp0_timestamp, code, requestId), cause);
  }
  invoke(message, id, requestId, code, cause, $super) {
    id = id === VOID ? '' : id;
    requestId = requestId === VOID ? '' : requestId;
    code = code === VOID ? 500 : code;
    cause = cause === VOID ? null : cause;
    return $super === VOID ? this.ii(message, id, requestId, code, cause) : $super.ii.call(this, message, id, requestId, code, cause);
  }
}
class F2Exception extends RuntimeException {
  constructor(error, cause) {
    return new.target.mi(error, cause);
  }
  static mi(error, cause) {
    cause = cause === VOID ? null : cause;
    var $this = this.b9(error.message, cause);
    captureStack($this, $this.li_1);
    $this.error = error;
    return $this;
  }
  ni() {
    return this.error;
  }
}
class Match {}
function and(match) {
  return new AndMatch(listOf([this, match]));
}
function or(match) {
  return new OrMatch(listOf([this, match]));
}
class AndMatch {
  constructor(matches, negative) {
    negative = negative === VOID ? false : negative;
    this.pi_1 = matches;
    this.qi_1 = negative;
  }
  oi() {
    return this.qi_1;
  }
  map(transform) {
    // Inline function 'kotlin.collections.map' call
    var this_0 = this.pi_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination = ArrayList.o(collectionSizeOrDefault(this_0, 10));
    var _iterator__ex2g4s = this_0.k();
    while (_iterator__ex2g4s.l()) {
      var item = _iterator__ex2g4s.m();
      var tmp$ret$2 = item.map(transform);
      destination.j(tmp$ret$2);
    }
    return new AndMatch(destination, this.qi_1);
  }
  not() {
    return this.ri(VOID, !this.qi_1);
  }
  and(match) {
    return this.ri(plus(this.pi_1, match));
  }
  si(matches, negative) {
    return new AndMatch(matches, negative);
  }
  ri(matches, negative, $super) {
    matches = matches === VOID ? this.pi_1 : matches;
    negative = negative === VOID ? this.qi_1 : negative;
    return $super === VOID ? this.si(matches, negative) : $super.si.call(this, matches, negative);
  }
  toString() {
    return 'AndMatch(matches=' + toString(this.pi_1) + ', negative=' + this.qi_1 + ')';
  }
  hashCode() {
    var result = hashCode(this.pi_1);
    result = imul(result, 31) + getBooleanHashCode(this.qi_1) | 0;
    return result;
  }
  equals(other) {
    if (this === other)
      return true;
    if (!(other instanceof AndMatch))
      return false;
    if (!equals(this.pi_1, other.pi_1))
      return false;
    if (!(this.qi_1 === other.qi_1))
      return false;
    return true;
  }
  get negative() {
    return this.oi();
  }
}
class OrMatch {
  constructor(matches, negative) {
    negative = negative === VOID ? false : negative;
    this.ti_1 = matches;
    this.ui_1 = negative;
  }
  oi() {
    return this.ui_1;
  }
  map(transform) {
    // Inline function 'kotlin.collections.map' call
    var this_0 = this.ti_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination = ArrayList.o(collectionSizeOrDefault(this_0, 10));
    var _iterator__ex2g4s = this_0.k();
    while (_iterator__ex2g4s.l()) {
      var item = _iterator__ex2g4s.m();
      var tmp$ret$2 = item.map(transform);
      destination.j(tmp$ret$2);
    }
    return new OrMatch(destination, this.ui_1);
  }
  not() {
    return this.vi(VOID, !this.ui_1);
  }
  or(match) {
    return this.vi(plus(this.ti_1, match));
  }
  si(matches, negative) {
    return new OrMatch(matches, negative);
  }
  vi(matches, negative, $super) {
    matches = matches === VOID ? this.ti_1 : matches;
    negative = negative === VOID ? this.ui_1 : negative;
    return $super === VOID ? this.si(matches, negative) : $super.si.call(this, matches, negative);
  }
  toString() {
    return 'OrMatch(matches=' + toString(this.ti_1) + ', negative=' + this.ui_1 + ')';
  }
  hashCode() {
    var result = hashCode(this.ti_1);
    result = imul(result, 31) + getBooleanHashCode(this.ui_1) | 0;
    return result;
  }
  equals(other) {
    if (this === other)
      return true;
    if (!(other instanceof OrMatch))
      return false;
    if (!equals(this.ti_1, other.ti_1))
      return false;
    if (!(this.ui_1 === other.ui_1))
      return false;
    return true;
  }
  get negative() {
    return this.oi();
  }
}
class SortDTO {}
class PageDTO {}
class Companion_2 {
  constructor() {
    Companion_instance_2 = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('f2.dsl.cqrs.page.Page', null, 2);
    tmp0_serialDesc.ch('total', false);
    tmp0_serialDesc.ch('items', false);
    this.bj_1 = tmp0_serialDesc;
  }
}
class Page {
  constructor(total, items) {
    Companion_getInstance_3();
    this.cj_1 = total;
    this.dj_1 = items;
  }
  zi() {
    return this.cj_1;
  }
  aj() {
    return this.dj_1;
  }
  static ej(seen0, total, items, serializationConstructorMarker) {
    Companion_getInstance_3();
    if (!(3 === (3 & seen0))) {
      throwMissingFieldException(seen0, 3, Companion_getInstance_3().bj_1);
    }
    var $this = createThis(this);
    $this.cj_1 = total;
    $this.dj_1 = items;
    return $this;
  }
  get total() {
    return this.zi();
  }
  get items() {
    return this.aj();
  }
}
class PageQueryDTO {}
class PageQueryResultDTO {}
class Companion_3 {
  constructor() {
    Companion_instance_3 = this;
    var tmp = this;
    var tmp_0 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    tmp.gj_1 = [lazy(tmp_0, PageQuery$Companion$$childSerializers$_anonymous__54lki7)];
  }
}
class $serializer_0 {
  constructor() {
    $serializer_instance_0 = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('f2.dsl.cqrs.page.PageQuery', this, 1);
    tmp0_serialDesc.ch('pagination', false);
    this.hj_1 = tmp0_serialDesc;
  }
  vd() {
    return this.hj_1;
  }
  zg() {
    var tmp0_cached = Companion_getInstance_4().gj_1;
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [get_nullable(tmp0_cached[0].i1())];
  }
}
class PageQuery {
  constructor(pagination) {
    Companion_getInstance_4();
    this.ij_1 = pagination;
  }
  fj() {
    return this.ij_1;
  }
  static jj(seen0, pagination, serializationConstructorMarker) {
    Companion_getInstance_4();
    if (!(1 === (1 & seen0))) {
      throwMissingFieldException(seen0, 1, $serializer_getInstance_0().hj_1);
    }
    var $this = createThis(this);
    $this.ij_1 = pagination;
    return $this;
  }
  get pagination() {
    return this.fj();
  }
}
class Companion_4 {
  constructor() {
    Companion_instance_4 = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('f2.dsl.cqrs.page.PageQueryResult', null, 3);
    tmp0_serialDesc.ch('pagination', false);
    tmp0_serialDesc.ch('total', false);
    tmp0_serialDesc.ch('items', false);
    this.kj_1 = tmp0_serialDesc;
  }
}
class PageQueryResult {
  constructor(pagination, total, items) {
    Companion_getInstance_5();
    this.lj_1 = pagination;
    this.mj_1 = total;
    this.nj_1 = items;
  }
  fj() {
    return this.lj_1;
  }
  zi() {
    return this.mj_1;
  }
  aj() {
    return this.nj_1;
  }
  static oj(seen0, pagination, total, items, serializationConstructorMarker) {
    Companion_getInstance_5();
    if (!(7 === (7 & seen0))) {
      throwMissingFieldException(seen0, 7, Companion_getInstance_5().kj_1);
    }
    var $this = createThis(this);
    $this.lj_1 = pagination;
    $this.mj_1 = total;
    $this.nj_1 = items;
    return $this;
  }
  get pagination() {
    return this.fj();
  }
  get total() {
    return this.zi();
  }
  get items() {
    return this.aj();
  }
}
class Companion_5 {}
class Pagination {}
class OffsetPaginationDTO {}
class PagePaginationDTO {}
class Companion_6 {}
class $serializer_1 {
  constructor() {
    $serializer_instance_1 = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('f2.dsl.cqrs.page.OffsetPagination', this, 2);
    tmp0_serialDesc.ch('offset', false);
    tmp0_serialDesc.ch('limit', false);
    this.sj_1 = tmp0_serialDesc;
  }
  vd() {
    return this.sj_1;
  }
  zg() {
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [IntSerializer_getInstance(), IntSerializer_getInstance()];
  }
}
class OffsetPagination {
  constructor(offset, limit) {
    this.tj_1 = offset;
    this.uj_1 = limit;
  }
  pj() {
    return this.tj_1;
  }
  qj() {
    return this.uj_1;
  }
  static vj(seen0, offset, limit, serializationConstructorMarker) {
    if (!(3 === (3 & seen0))) {
      throwMissingFieldException(seen0, 3, $serializer_getInstance_1().sj_1);
    }
    var $this = createThis(this);
    $this.tj_1 = offset;
    $this.uj_1 = limit;
    return $this;
  }
  get offset() {
    return this.pj();
  }
  get limit() {
    return this.qj();
  }
}
class Companion_7 {}
class $serializer_2 {
  constructor() {
    $serializer_instance_2 = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('f2.dsl.cqrs.page.PagePagination', this, 2);
    tmp0_serialDesc.ch('page', false);
    tmp0_serialDesc.ch('size', false);
    this.wj_1 = tmp0_serialDesc;
  }
  vd() {
    return this.wj_1;
  }
  zg() {
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [get_nullable(IntSerializer_getInstance()), get_nullable(IntSerializer_getInstance())];
  }
}
class PagePagination {
  constructor(page, size) {
    this.xj_1 = page;
    this.yj_1 = size;
  }
  rj() {
    return this.xj_1;
  }
  n() {
    return this.yj_1;
  }
  static zj(seen0, page, size, serializationConstructorMarker) {
    if (!(3 === (3 & seen0))) {
      throwMissingFieldException(seen0, 3, $serializer_getInstance_2().wj_1);
    }
    var $this = createThis(this);
    $this.xj_1 = page;
    $this.yj_1 = size;
    return $this;
  }
  get page() {
    return this.rj();
  }
  get size() {
    return this.n();
  }
}
//endregion
var Companion_instance;
function Companion_getInstance_0() {
  if (Companion_instance === VOID)
    new Companion();
  return Companion_instance;
}
var Companion_instance_0;
function Companion_getInstance_1() {
  return Companion_instance_0;
}
var $serializer_instance;
function $serializer_getInstance() {
  if ($serializer_instance === VOID)
    new $serializer();
  return $serializer_instance;
}
var Companion_instance_1;
function Companion_getInstance_2() {
  return Companion_instance_1;
}
var Companion_instance_2;
function Companion_getInstance_3() {
  if (Companion_instance_2 === VOID)
    new Companion_2();
  return Companion_instance_2;
}
function PageQuery$Companion$$childSerializers$_anonymous__54lki7() {
  var tmp = getKClass(OffsetPaginationDTO);
  // Inline function 'kotlin.arrayOf' call
  // Inline function 'kotlin.js.unsafeCast' call
  // Inline function 'kotlin.js.asDynamic' call
  var tmp$ret$0 = [];
  return PolymorphicSerializer.ke(tmp, tmp$ret$0);
}
var Companion_instance_3;
function Companion_getInstance_4() {
  if (Companion_instance_3 === VOID)
    new Companion_3();
  return Companion_instance_3;
}
var $serializer_instance_0;
function $serializer_getInstance_0() {
  if ($serializer_instance_0 === VOID)
    new $serializer_0();
  return $serializer_instance_0;
}
var Companion_instance_4;
function Companion_getInstance_5() {
  if (Companion_instance_4 === VOID)
    new Companion_4();
  return Companion_instance_4;
}
var Companion_instance_5;
function Companion_getInstance_6() {
  return Companion_instance_5;
}
var Companion_instance_6;
function Companion_getInstance_7() {
  return Companion_instance_6;
}
var $serializer_instance_1;
function $serializer_getInstance_1() {
  if ($serializer_instance_1 === VOID)
    new $serializer_1();
  return $serializer_instance_1;
}
var Companion_instance_7;
function Companion_getInstance_8() {
  return Companion_instance_7;
}
var $serializer_instance_2;
function $serializer_getInstance_2() {
  if ($serializer_instance_2 === VOID)
    new $serializer_2();
  return $serializer_instance_2;
}
//region block: post-declaration
initMetadataForInterface(Message, 'Message');
initMetadataForInterface(Command, 'Command', VOID, VOID, [Message]);
initMetadataForInterface(Event, 'Event', VOID, VOID, [Message]);
initMetadataForInterface(Problem, 'Problem', VOID, VOID, [Message]);
initMetadataForInterface(Query, 'Query', VOID, VOID, [Message]);
initMetadataForInterface(WithEnvelopeId, 'WithEnvelopeId');
initMetadataForInterface(WithEnvelopeData, 'WithEnvelopeData');
initMetadataForInterface(EnvelopeDTO, 'EnvelopeDTO', VOID, VOID, [WithEnvelopeId, WithEnvelopeData, Message]);
initMetadataForCompanion(Companion);
initMetadataForClass(Envelope, 'Envelope', VOID, VOID, [EnvelopeDTO], VOID, VOID, {0: Companion_getInstance_0});
initMetadataForInterface(F2ErrorDTO, 'F2ErrorDTO', VOID, VOID, [Problem]);
initMetadataForCompanion(Companion_0);
protoOf($serializer).ah = typeParametersSerializers;
initMetadataForObject($serializer, '$serializer', VOID, VOID, [GeneratedSerializer]);
initMetadataForClass(F2Error, 'F2Error', VOID, VOID, [F2ErrorDTO], VOID, VOID, {0: $serializer_getInstance});
initMetadataForCompanion(Companion_1);
initMetadataForClass(F2Exception, 'F2Exception');
initMetadataForInterface(Match, 'Match');
protoOf(AndMatch).or = or;
initMetadataForClass(AndMatch, 'AndMatch', VOID, VOID, [Match]);
protoOf(OrMatch).and = and;
initMetadataForClass(OrMatch, 'OrMatch', VOID, VOID, [Match]);
initMetadataForInterface(SortDTO, 'SortDTO');
initMetadataForInterface(PageDTO, 'PageDTO');
initMetadataForCompanion(Companion_2);
initMetadataForClass(Page, 'Page', VOID, VOID, [PageDTO], VOID, VOID, {0: Companion_getInstance_3});
initMetadataForInterface(PageQueryDTO, 'PageQueryDTO', VOID, VOID, [Query]);
initMetadataForInterface(PageQueryResultDTO, 'PageQueryResultDTO', VOID, VOID, [Event, PageDTO]);
initMetadataForCompanion(Companion_3);
protoOf($serializer_0).ah = typeParametersSerializers;
initMetadataForObject($serializer_0, '$serializer', VOID, VOID, [GeneratedSerializer]);
initMetadataForClass(PageQuery, 'PageQuery', VOID, VOID, [PageQueryDTO], VOID, VOID, {0: $serializer_getInstance_0});
initMetadataForCompanion(Companion_4);
initMetadataForClass(PageQueryResult, 'PageQueryResult', VOID, VOID, [PageQueryResultDTO], VOID, VOID, {0: Companion_getInstance_5});
initMetadataForCompanion(Companion_5);
initMetadataForInterface(Pagination, 'Pagination', VOID, VOID, VOID, VOID, VOID, {0: Companion_getInstance_6});
initMetadataForInterface(OffsetPaginationDTO, 'OffsetPaginationDTO', VOID, VOID, [Pagination]);
initMetadataForInterface(PagePaginationDTO, 'PagePaginationDTO', VOID, VOID, [Pagination]);
initMetadataForCompanion(Companion_6);
protoOf($serializer_1).ah = typeParametersSerializers;
initMetadataForObject($serializer_1, '$serializer', VOID, VOID, [GeneratedSerializer]);
initMetadataForClass(OffsetPagination, 'OffsetPagination', VOID, VOID, [OffsetPaginationDTO], VOID, VOID, {0: $serializer_getInstance_1});
initMetadataForCompanion(Companion_7);
protoOf($serializer_2).ah = typeParametersSerializers;
initMetadataForObject($serializer_2, '$serializer', VOID, VOID, [GeneratedSerializer]);
initMetadataForClass(PagePagination, 'PagePagination', VOID, VOID, [PagePaginationDTO], VOID, VOID, {0: $serializer_getInstance_2});
//endregion
//region block: init
Companion_instance_0 = new Companion_0();
Companion_instance_1 = new Companion_1();
Companion_instance_5 = new Companion_5();
Companion_instance_6 = new Companion_6();
Companion_instance_7 = new Companion_7();
//endregion
//region block: exports
defineProp(F2Exception, 'Companion', Companion_getInstance_2, VOID, true);
export {
  Envelope as Envelope,
  F2Error as F2Error,
  F2Exception as F2Exception,
  Page as Page,
  PageQuery as PageQuery,
  PageQueryResult as PageQueryResult,
  OffsetPagination as OffsetPagination,
  PagePagination as PagePagination,
};
export {
  Command as Commandj5jg47cicltn,
  Event as Eventox8naz25uncx,
};
//endregion

//# sourceMappingURL=fixers-f2-f2-dsl-f2-dsl-cqrs.mjs.map

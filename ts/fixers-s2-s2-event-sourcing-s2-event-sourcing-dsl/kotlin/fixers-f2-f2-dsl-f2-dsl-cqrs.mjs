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
    tmp0_serialDesc.ii('id', false);
    tmp0_serialDesc.ii('data', false);
    tmp0_serialDesc.ii('type', false);
    tmp0_serialDesc.ii('datacontenttype', true);
    tmp0_serialDesc.ii('specversion', true);
    tmp0_serialDesc.ii('source', true);
    tmp0_serialDesc.ii('time', true);
    this.vi_1 = tmp0_serialDesc;
  }
}
class Envelope {
  constructor(id, data, type, datacontenttype, specversion, source, time) {
    Companion_getInstance_0();
    datacontenttype = datacontenttype === VOID ? null : datacontenttype;
    specversion = specversion === VOID ? null : specversion;
    source = source === VOID ? null : source;
    time = time === VOID ? null : time;
    this.wi_1 = id;
    this.xi_1 = data;
    this.yi_1 = type;
    this.zi_1 = datacontenttype;
    this.aj_1 = specversion;
    this.bj_1 = source;
    this.cj_1 = time;
  }
  oi() {
    return this.wi_1;
  }
  pi() {
    return this.xi_1;
  }
  qi() {
    return this.yi_1;
  }
  ri() {
    return this.zi_1;
  }
  si() {
    return this.aj_1;
  }
  ti() {
    return this.bj_1;
  }
  ui() {
    return this.cj_1;
  }
  static dj(seen0, id, data, type, datacontenttype, specversion, source, time, serializationConstructorMarker) {
    Companion_getInstance_0();
    if (!(7 === (7 & seen0))) {
      throwMissingFieldException(seen0, 7, Companion_getInstance_0().vi_1);
    }
    var $this = createThis(this);
    $this.wi_1 = id;
    $this.xi_1 = data;
    $this.yi_1 = type;
    if (0 === (seen0 & 8))
      $this.zi_1 = null;
    else
      $this.zi_1 = datacontenttype;
    if (0 === (seen0 & 16))
      $this.aj_1 = null;
    else
      $this.aj_1 = specversion;
    if (0 === (seen0 & 32))
      $this.bj_1 = null;
    else
      $this.bj_1 = source;
    if (0 === (seen0 & 64))
      $this.cj_1 = null;
    else
      $this.cj_1 = time;
    return $this;
  }
  get id() {
    return this.oi();
  }
  get data() {
    return this.pi();
  }
  get type() {
    return this.qi();
  }
  get datacontenttype() {
    return this.ri();
  }
  get specversion() {
    return this.si();
  }
  get source() {
    return this.ti();
  }
  get time() {
    return this.ui();
  }
}
class F2ErrorDTO {}
class Companion_0 {}
class $serializer {
  constructor() {
    $serializer_instance = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('f2.dsl.cqrs.error.F2Error', this, 5);
    tmp0_serialDesc.ii('message', false);
    tmp0_serialDesc.ii('id', true);
    tmp0_serialDesc.ii('timestamp', true);
    tmp0_serialDesc.ii('code', true);
    tmp0_serialDesc.ii('requestId', true);
    this.hj_1 = tmp0_serialDesc;
  }
  bf() {
    return this.hj_1;
  }
  fi() {
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [StringSerializer_getInstance(), get_nullable(StringSerializer_getInstance()), StringSerializer_getInstance(), IntSerializer_getInstance(), get_nullable(StringSerializer_getInstance())];
  }
}
class F2Error {
  constructor(message, id, timestamp, code, requestId) {
    id = id === VOID ? Companion_getInstance().ve().toString() : id;
    timestamp = timestamp === VOID ? System_instance.zd().toString() : timestamp;
    code = code === VOID ? 500 : code;
    requestId = requestId === VOID ? null : requestId;
    this.ij_1 = message;
    this.jj_1 = id;
    this.kj_1 = timestamp;
    this.lj_1 = code;
    this.mj_1 = requestId;
  }
  z9() {
    return this.ij_1;
  }
  oi() {
    return this.jj_1;
  }
  ej() {
    return this.kj_1;
  }
  fj() {
    return this.lj_1;
  }
  gj() {
    return this.mj_1;
  }
  toString() {
    return "F2Error(timestamp='" + this.timestamp + "', code=" + this.code + ", requestId='" + this.requestId + "', message='" + this.message + "')";
  }
  static nj(seen0, message, id, timestamp, code, requestId, serializationConstructorMarker) {
    if (!(1 === (1 & seen0))) {
      throwMissingFieldException(seen0, 1, $serializer_getInstance().hj_1);
    }
    var $this = createThis(this);
    $this.ij_1 = message;
    if (0 === (seen0 & 2))
      $this.jj_1 = Companion_getInstance().ve().toString();
    else
      $this.jj_1 = id;
    if (0 === (seen0 & 4))
      $this.kj_1 = System_instance.zd().toString();
    else
      $this.kj_1 = timestamp;
    if (0 === (seen0 & 8))
      $this.lj_1 = 500;
    else
      $this.lj_1 = code;
    if (0 === (seen0 & 16))
      $this.mj_1 = null;
    else
      $this.mj_1 = requestId;
    return $this;
  }
  get message() {
    return this.z9();
  }
  get id() {
    return this.oi();
  }
  get timestamp() {
    return this.ej();
  }
  get code() {
    return this.fj();
  }
  get requestId() {
    return this.gj();
  }
}
class Companion_1 {
  oj(message, id, requestId, code, cause) {
    var tmp0_timestamp = System_instance.zd().toString();
    return F2Exception.sj(new F2Error(message, id, tmp0_timestamp, code, requestId), cause);
  }
  invoke(message, id, requestId, code, cause, $super) {
    id = id === VOID ? '' : id;
    requestId = requestId === VOID ? '' : requestId;
    code = code === VOID ? 500 : code;
    cause = cause === VOID ? null : cause;
    return $super === VOID ? this.oj(message, id, requestId, code, cause) : $super.oj.call(this, message, id, requestId, code, cause);
  }
}
class F2Exception extends RuntimeException {
  constructor(error, cause) {
    return new.target.sj(error, cause);
  }
  static sj(error, cause) {
    cause = cause === VOID ? null : cause;
    var $this = this.da(error.message, cause);
    captureStack($this, $this.rj_1);
    $this.error = error;
    return $this;
  }
  tj() {
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
    this.vj_1 = matches;
    this.wj_1 = negative;
  }
  uj() {
    return this.wj_1;
  }
  map(transform) {
    // Inline function 'kotlin.collections.map' call
    var this_0 = this.vj_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination = ArrayList.o(collectionSizeOrDefault(this_0, 10));
    var _iterator__ex2g4s = this_0.k();
    while (_iterator__ex2g4s.l()) {
      var item = _iterator__ex2g4s.m();
      var tmp$ret$2 = item.map(transform);
      destination.j(tmp$ret$2);
    }
    return new AndMatch(destination, this.wj_1);
  }
  not() {
    return this.xj(VOID, !this.wj_1);
  }
  and(match) {
    return this.xj(plus(this.vj_1, match));
  }
  yj(matches, negative) {
    return new AndMatch(matches, negative);
  }
  xj(matches, negative, $super) {
    matches = matches === VOID ? this.vj_1 : matches;
    negative = negative === VOID ? this.wj_1 : negative;
    return $super === VOID ? this.yj(matches, negative) : $super.yj.call(this, matches, negative);
  }
  toString() {
    return 'AndMatch(matches=' + toString(this.vj_1) + ', negative=' + this.wj_1 + ')';
  }
  hashCode() {
    var result = hashCode(this.vj_1);
    result = imul(result, 31) + getBooleanHashCode(this.wj_1) | 0;
    return result;
  }
  equals(other) {
    if (this === other)
      return true;
    if (!(other instanceof AndMatch))
      return false;
    if (!equals(this.vj_1, other.vj_1))
      return false;
    if (!(this.wj_1 === other.wj_1))
      return false;
    return true;
  }
  get negative() {
    return this.uj();
  }
}
class OrMatch {
  constructor(matches, negative) {
    negative = negative === VOID ? false : negative;
    this.zj_1 = matches;
    this.ak_1 = negative;
  }
  uj() {
    return this.ak_1;
  }
  map(transform) {
    // Inline function 'kotlin.collections.map' call
    var this_0 = this.zj_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination = ArrayList.o(collectionSizeOrDefault(this_0, 10));
    var _iterator__ex2g4s = this_0.k();
    while (_iterator__ex2g4s.l()) {
      var item = _iterator__ex2g4s.m();
      var tmp$ret$2 = item.map(transform);
      destination.j(tmp$ret$2);
    }
    return new OrMatch(destination, this.ak_1);
  }
  not() {
    return this.bk(VOID, !this.ak_1);
  }
  or(match) {
    return this.bk(plus(this.zj_1, match));
  }
  yj(matches, negative) {
    return new OrMatch(matches, negative);
  }
  bk(matches, negative, $super) {
    matches = matches === VOID ? this.zj_1 : matches;
    negative = negative === VOID ? this.ak_1 : negative;
    return $super === VOID ? this.yj(matches, negative) : $super.yj.call(this, matches, negative);
  }
  toString() {
    return 'OrMatch(matches=' + toString(this.zj_1) + ', negative=' + this.ak_1 + ')';
  }
  hashCode() {
    var result = hashCode(this.zj_1);
    result = imul(result, 31) + getBooleanHashCode(this.ak_1) | 0;
    return result;
  }
  equals(other) {
    if (this === other)
      return true;
    if (!(other instanceof OrMatch))
      return false;
    if (!equals(this.zj_1, other.zj_1))
      return false;
    if (!(this.ak_1 === other.ak_1))
      return false;
    return true;
  }
  get negative() {
    return this.uj();
  }
}
class SortDTO {}
class PageDTO {}
class Companion_2 {
  constructor() {
    Companion_instance_2 = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('f2.dsl.cqrs.page.Page', null, 2);
    tmp0_serialDesc.ii('total', false);
    tmp0_serialDesc.ii('items', false);
    this.hk_1 = tmp0_serialDesc;
  }
}
class Page {
  constructor(total, items) {
    Companion_getInstance_3();
    this.ik_1 = total;
    this.jk_1 = items;
  }
  fk() {
    return this.ik_1;
  }
  gk() {
    return this.jk_1;
  }
  static kk(seen0, total, items, serializationConstructorMarker) {
    Companion_getInstance_3();
    if (!(3 === (3 & seen0))) {
      throwMissingFieldException(seen0, 3, Companion_getInstance_3().hk_1);
    }
    var $this = createThis(this);
    $this.ik_1 = total;
    $this.jk_1 = items;
    return $this;
  }
  get total() {
    return this.fk();
  }
  get items() {
    return this.gk();
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
    tmp.mk_1 = [lazy(tmp_0, PageQuery$Companion$$childSerializers$_anonymous__54lki7)];
  }
}
class $serializer_0 {
  constructor() {
    $serializer_instance_0 = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('f2.dsl.cqrs.page.PageQuery', this, 1);
    tmp0_serialDesc.ii('pagination', false);
    this.nk_1 = tmp0_serialDesc;
  }
  bf() {
    return this.nk_1;
  }
  fi() {
    var tmp0_cached = Companion_getInstance_4().mk_1;
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [get_nullable(tmp0_cached[0].i1())];
  }
}
class PageQuery {
  constructor(pagination) {
    Companion_getInstance_4();
    this.ok_1 = pagination;
  }
  lk() {
    return this.ok_1;
  }
  static pk(seen0, pagination, serializationConstructorMarker) {
    Companion_getInstance_4();
    if (!(1 === (1 & seen0))) {
      throwMissingFieldException(seen0, 1, $serializer_getInstance_0().nk_1);
    }
    var $this = createThis(this);
    $this.ok_1 = pagination;
    return $this;
  }
  get pagination() {
    return this.lk();
  }
}
class Companion_4 {
  constructor() {
    Companion_instance_4 = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('f2.dsl.cqrs.page.PageQueryResult', null, 3);
    tmp0_serialDesc.ii('pagination', false);
    tmp0_serialDesc.ii('total', false);
    tmp0_serialDesc.ii('items', false);
    this.qk_1 = tmp0_serialDesc;
  }
}
class PageQueryResult {
  constructor(pagination, total, items) {
    Companion_getInstance_5();
    this.rk_1 = pagination;
    this.sk_1 = total;
    this.tk_1 = items;
  }
  lk() {
    return this.rk_1;
  }
  fk() {
    return this.sk_1;
  }
  gk() {
    return this.tk_1;
  }
  static uk(seen0, pagination, total, items, serializationConstructorMarker) {
    Companion_getInstance_5();
    if (!(7 === (7 & seen0))) {
      throwMissingFieldException(seen0, 7, Companion_getInstance_5().qk_1);
    }
    var $this = createThis(this);
    $this.rk_1 = pagination;
    $this.sk_1 = total;
    $this.tk_1 = items;
    return $this;
  }
  get pagination() {
    return this.lk();
  }
  get total() {
    return this.fk();
  }
  get items() {
    return this.gk();
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
    tmp0_serialDesc.ii('offset', false);
    tmp0_serialDesc.ii('limit', false);
    this.yk_1 = tmp0_serialDesc;
  }
  bf() {
    return this.yk_1;
  }
  fi() {
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [IntSerializer_getInstance(), IntSerializer_getInstance()];
  }
}
class OffsetPagination {
  constructor(offset, limit) {
    this.zk_1 = offset;
    this.al_1 = limit;
  }
  vk() {
    return this.zk_1;
  }
  wk() {
    return this.al_1;
  }
  static bl(seen0, offset, limit, serializationConstructorMarker) {
    if (!(3 === (3 & seen0))) {
      throwMissingFieldException(seen0, 3, $serializer_getInstance_1().yk_1);
    }
    var $this = createThis(this);
    $this.zk_1 = offset;
    $this.al_1 = limit;
    return $this;
  }
  get offset() {
    return this.vk();
  }
  get limit() {
    return this.wk();
  }
}
class Companion_7 {}
class $serializer_2 {
  constructor() {
    $serializer_instance_2 = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('f2.dsl.cqrs.page.PagePagination', this, 2);
    tmp0_serialDesc.ii('page', false);
    tmp0_serialDesc.ii('size', false);
    this.cl_1 = tmp0_serialDesc;
  }
  bf() {
    return this.cl_1;
  }
  fi() {
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [get_nullable(IntSerializer_getInstance()), get_nullable(IntSerializer_getInstance())];
  }
}
class PagePagination {
  constructor(page, size) {
    this.dl_1 = page;
    this.el_1 = size;
  }
  xk() {
    return this.dl_1;
  }
  n() {
    return this.el_1;
  }
  static fl(seen0, page, size, serializationConstructorMarker) {
    if (!(3 === (3 & seen0))) {
      throwMissingFieldException(seen0, 3, $serializer_getInstance_2().cl_1);
    }
    var $this = createThis(this);
    $this.dl_1 = page;
    $this.el_1 = size;
    return $this;
  }
  get page() {
    return this.xk();
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
  return PolymorphicSerializer.qf(tmp, tmp$ret$0);
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
protoOf($serializer).gi = typeParametersSerializers;
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
protoOf($serializer_0).gi = typeParametersSerializers;
initMetadataForObject($serializer_0, '$serializer', VOID, VOID, [GeneratedSerializer]);
initMetadataForClass(PageQuery, 'PageQuery', VOID, VOID, [PageQueryDTO], VOID, VOID, {0: $serializer_getInstance_0});
initMetadataForCompanion(Companion_4);
initMetadataForClass(PageQueryResult, 'PageQueryResult', VOID, VOID, [PageQueryResultDTO], VOID, VOID, {0: Companion_getInstance_5});
initMetadataForCompanion(Companion_5);
initMetadataForInterface(Pagination, 'Pagination', VOID, VOID, VOID, VOID, VOID, {0: Companion_getInstance_6});
initMetadataForInterface(OffsetPaginationDTO, 'OffsetPaginationDTO', VOID, VOID, [Pagination]);
initMetadataForInterface(PagePaginationDTO, 'PagePaginationDTO', VOID, VOID, [Pagination]);
initMetadataForCompanion(Companion_6);
protoOf($serializer_1).gi = typeParametersSerializers;
initMetadataForObject($serializer_1, '$serializer', VOID, VOID, [GeneratedSerializer]);
initMetadataForClass(OffsetPagination, 'OffsetPagination', VOID, VOID, [OffsetPaginationDTO], VOID, VOID, {0: $serializer_getInstance_1});
initMetadataForCompanion(Companion_7);
protoOf($serializer_2).gi = typeParametersSerializers;
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

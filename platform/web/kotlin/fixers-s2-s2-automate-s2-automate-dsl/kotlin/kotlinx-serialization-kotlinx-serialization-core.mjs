import {
  initMetadataForInterface1egvbzx539z91 as initMetadataForInterface,
  StringCompanionObject_instance2dx16b5pp3v3z as StringCompanionObject_instance,
  Unit_instance3kz1zqli6gr1r as Unit_instance,
  constructCallableReference23y65rf941mch as constructCallableReference,
  emptyList1g2z5xcrvp2zy as emptyList,
  LazyThreadSafetyMode_PUBLICATION_getInstance23f213579at67 as LazyThreadSafetyMode_PUBLICATION_getInstance,
  lazy1261dae0bgscp as lazy,
  asList2ho2pewtsfvv as asList,
  KProperty1ca4yb4wlo496 as KProperty1,
  getPropertyCallableRef3hckxc0xueiaj as getPropertyCallableRef,
  toString1pkumu07cwy4m as toString,
  initMetadataForClassbxx6q50dy2s7 as initMetadataForClass,
  captureStack1fzi4aczwc4hg as captureStack,
  IllegalArgumentException2asla15b5jaob as IllegalArgumentException,
  THROW_CCE2g6jy02ryeudk as THROW_CCE,
  isInterface3d6p8outrmvmk as isInterface,
  equals2au1ep9vhcato as equals,
  getStringHashCode26igk1bx568vk as getStringHashCode,
  VOID3gxj6tk5isa35 as VOID,
  isBlank1dvkhjjvox3p0 as isBlank,
  toList383f556t1dixk as toList,
  ArrayList3it5z8td81qkl as ArrayList,
  HashSet2dzve9y63nf0v as HashSet,
  toHashSet1qrcsl3g8ugc8 as toHashSet,
  copyToArray2j022khrow2yi as copyToArray,
  toBooleanArray2u3qw7fjwsmuh as toBooleanArray,
  withIndex3s8q7w1g0hyfn as withIndex,
  collectionSizeOrDefault36dulx8yinfqm as collectionSizeOrDefault,
  to2cs3ny02qtbcb as to,
  toMap1vec9topfei08 as toMap,
  lazy2hsh8ze7j6ikd as lazy_0,
  contentEqualsaf55p28mnw74 as contentEquals,
  protoOf180f3jzyo7rfj as protoOf,
  initMetadataForObject1cxne3s9w65el as initMetadataForObject,
  getKClassFromExpression348iqjl4fnx2f as getKClassFromExpression,
  ensureNotNull1e947j3ixpazm as ensureNotNull,
  createThis2j2avj17cvnv2 as createThis,
  hashCodeq5arwsb9dgti as hashCode,
  IndexOutOfBoundsException1qfr429iumro0 as IndexOutOfBoundsException,
  HashMap1a0ld5kgwhmhv as HashMap,
  booleanArray2jdug9b51huk7 as booleanArray,
  emptyMapr06gerzljqtm as emptyMap,
  contentHashCode2i020q5tbeh2s as contentHashCode,
  until1jbpn0z3f8lbg as until,
  joinToString1cxrrlmo0chqs as joinToString,
  IllegalStateExceptionkoljg5n0nrlr as IllegalStateException,
  get_indicesc04v40g017hw as get_indices,
} from './kotlin-kotlin-stdlib.mjs';
//region block: imports
var imul = Math.imul;
//endregion
//region block: pre-declaration
class KSerializer {}
class AbstractPolymorphicSerializer {
  static je() {
    return createThis(this);
  }
}
class PolymorphicSerializer extends AbstractPolymorphicSerializer {
  constructor(baseClass) {
    return new.target.ie(baseClass);
  }
  static ie(baseClass) {
    var $this = this.je();
    $this.fe_1 = baseClass;
    $this.ge_1 = emptyList();
    var tmp = $this;
    var tmp_0 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    tmp.he_1 = lazy(tmp_0, PolymorphicSerializer$descriptor$delegate$lambda($this));
    return $this;
  }
  static ke(baseClass, classAnnotations) {
    var $this = this.ie(baseClass);
    $this.ge_1 = asList(classAnnotations);
    return $this;
  }
  vd() {
    var tmp0 = this.he_1;
    var tmp = KProperty1;
    // Inline function 'kotlin.getValue' call
    getPropertyCallableRef('descriptor', 1, tmp, PolymorphicSerializer$_get_descriptor_$ref_8tw9if(), null);
    return tmp0.i1();
  }
  toString() {
    return 'kotlinx.serialization.PolymorphicSerializer(baseClass: ' + toString(this.fe_1) + ')';
  }
}
class SerializationException extends IllegalArgumentException {
  static pe() {
    var $this = this.z8();
    init_kotlinx_serialization_SerializationException($this);
    return $this;
  }
  static qe(message, cause) {
    var $this = this.a9(message, cause);
    init_kotlinx_serialization_SerializationException($this);
    return $this;
  }
}
class MissingFieldException extends SerializationException {
  constructor(message, cause, missingFields, serialName) {
    return new.target.ye(message, cause, missingFields, serialName);
  }
  static ye(message, cause, missingFields, serialName) {
    var $this = this.qe(message, cause);
    captureStack($this, $this.xe_1);
    $this.ve_1 = missingFields;
    $this.we_1 = serialName;
    return $this;
  }
  static ze(missingFields, serialName) {
    var tmp0_message = missingFields.n() === 1 ? "Field '" + missingFields.s(0) + "' is required for type with serial name '" + serialName + "', but it was missing" : 'Fields ' + toString(missingFields) + " are required for type with serial name '" + serialName + "', but they were missing";
    return this.ye(tmp0_message, null, missingFields, serialName);
  }
}
class SerialDescriptor {}
function get_isNullable() {
  return false;
}
class ContextDescriptor {
  constructor(original, kClass) {
    this.bf_1 = original;
    this.cf_1 = kClass;
    this.df_1 = this.bf_1.ef() + '<' + this.cf_1.r9() + '>';
  }
  ef() {
    return this.df_1;
  }
  equals(other) {
    var tmp0_elvis_lhs = other instanceof ContextDescriptor ? other : null;
    var tmp;
    if (tmp0_elvis_lhs == null) {
      return false;
    } else {
      tmp = tmp0_elvis_lhs;
    }
    var another = tmp;
    return equals(this.bf_1, another.bf_1) && another.cf_1.equals(this.cf_1);
  }
  hashCode() {
    var result = this.cf_1.hashCode();
    result = imul(31, result) + getStringHashCode(this.df_1) | 0;
    return result;
  }
  toString() {
    return 'ContextDescriptor(kClass: ' + toString(this.cf_1) + ', original: ' + toString(this.bf_1) + ')';
  }
  ff() {
    return this.bf_1.ff();
  }
  af() {
    return this.bf_1.af();
  }
  gf() {
    return this.bf_1.gf();
  }
  hf(index) {
    return this.bf_1.hf(index);
  }
  if(index) {
    return this.bf_1.if(index);
  }
}
class elementDescriptors$1 {
  constructor($this_elementDescriptors) {
    this.kf_1 = $this_elementDescriptors;
    this.jf_1 = $this_elementDescriptors.gf();
  }
  l() {
    return this.jf_1 > 0;
  }
  m() {
    var tmp = this.kf_1.gf();
    var _unary__edvuaz = this.jf_1;
    this.jf_1 = _unary__edvuaz - 1 | 0;
    return this.kf_1.if(tmp - _unary__edvuaz | 0);
  }
}
class elementDescriptors$$inlined$Iterable$1 {
  constructor($this_elementDescriptors) {
    this.lf_1 = $this_elementDescriptors;
  }
  k() {
    return new elementDescriptors$1(this.lf_1);
  }
}
class ClassSerialDescriptorBuilder {
  constructor(serialName) {
    this.wd_1 = serialName;
    this.xd_1 = false;
    this.yd_1 = emptyList();
    this.zd_1 = ArrayList.i();
    this.ae_1 = HashSet.y6();
    this.be_1 = ArrayList.i();
    this.ce_1 = ArrayList.i();
    this.de_1 = ArrayList.i();
  }
  mf(elementName, descriptor, annotations, isOptional) {
    // Inline function 'kotlin.require' call
    if (!this.ae_1.j(elementName)) {
      var message = "Element with name '" + elementName + "' is already registered in " + this.wd_1;
      throw IllegalArgumentException.g3(toString(message));
    }
    // Inline function 'kotlin.collections.plusAssign' call
    this.zd_1.j(elementName);
    // Inline function 'kotlin.collections.plusAssign' call
    this.be_1.j(descriptor);
    // Inline function 'kotlin.collections.plusAssign' call
    this.ce_1.j(annotations);
    // Inline function 'kotlin.collections.plusAssign' call
    this.de_1.j(isOptional);
  }
  ee(elementName, descriptor, annotations, isOptional, $super) {
    annotations = annotations === VOID ? emptyList() : annotations;
    isOptional = isOptional === VOID ? false : isOptional;
    var tmp;
    if ($super === VOID) {
      this.mf(elementName, descriptor, annotations, isOptional);
      tmp = Unit_instance;
    } else {
      tmp = $super.mf.call(this, elementName, descriptor, annotations, isOptional);
    }
    return tmp;
  }
}
class CachedNames {}
class SerialDescriptorImpl {
  constructor(serialName, kind, elementsCount, typeParameters, builder) {
    this.nf_1 = serialName;
    this.of_1 = kind;
    this.pf_1 = elementsCount;
    this.qf_1 = builder.yd_1;
    this.rf_1 = toHashSet(builder.zd_1);
    var tmp = this;
    // Inline function 'kotlin.collections.toTypedArray' call
    var this_0 = builder.zd_1;
    tmp.sf_1 = copyToArray(this_0);
    this.tf_1 = compactArray(builder.be_1);
    var tmp_0 = this;
    // Inline function 'kotlin.collections.toTypedArray' call
    var this_1 = builder.ce_1;
    tmp_0.uf_1 = copyToArray(this_1);
    this.vf_1 = toBooleanArray(builder.de_1);
    var tmp_1 = this;
    // Inline function 'kotlin.collections.map' call
    var this_2 = withIndex(this.sf_1);
    // Inline function 'kotlin.collections.mapTo' call
    var destination = ArrayList.o(collectionSizeOrDefault(this_2, 10));
    var _iterator__ex2g4s = this_2.k();
    while (_iterator__ex2g4s.l()) {
      var item = _iterator__ex2g4s.m();
      var tmp$ret$4 = to(item.ub_1, item.tb_1);
      destination.j(tmp$ret$4);
    }
    tmp_1.wf_1 = toMap(destination);
    this.xf_1 = compactArray(typeParameters);
    var tmp_2 = this;
    tmp_2.yf_1 = lazy_0(SerialDescriptorImpl$_hashCode$delegate$lambda(this));
  }
  ef() {
    return this.nf_1;
  }
  ff() {
    return this.of_1;
  }
  gf() {
    return this.pf_1;
  }
  zf() {
    return this.rf_1;
  }
  hf(index) {
    return getChecked(this.sf_1, index);
  }
  if(index) {
    return getChecked(this.tf_1, index);
  }
  equals(other) {
    var tmp$ret$0;
    $l$block_5: {
      // Inline function 'kotlinx.serialization.internal.equalsImpl' call
      if (this === other) {
        tmp$ret$0 = true;
        break $l$block_5;
      }
      if (!(other instanceof SerialDescriptorImpl)) {
        tmp$ret$0 = false;
        break $l$block_5;
      }
      if (!(this.ef() === other.ef())) {
        tmp$ret$0 = false;
        break $l$block_5;
      }
      if (!contentEquals(this.xf_1, other.xf_1)) {
        tmp$ret$0 = false;
        break $l$block_5;
      }
      if (!(this.gf() === other.gf())) {
        tmp$ret$0 = false;
        break $l$block_5;
      }
      var inductionVariable = 0;
      var last = this.gf();
      if (inductionVariable < last)
        do {
          var index = inductionVariable;
          inductionVariable = inductionVariable + 1 | 0;
          if (!(this.if(index).ef() === other.if(index).ef())) {
            tmp$ret$0 = false;
            break $l$block_5;
          }
          if (!equals(this.if(index).ff(), other.if(index).ff())) {
            tmp$ret$0 = false;
            break $l$block_5;
          }
        }
         while (inductionVariable < last);
      tmp$ret$0 = true;
    }
    return tmp$ret$0;
  }
  hashCode() {
    return _get__hashCode__tgwhef(this);
  }
  toString() {
    return toStringImpl(this);
  }
}
class SerialKind {
  toString() {
    return ensureNotNull(getKClassFromExpression(this).r9());
  }
  hashCode() {
    return getStringHashCode(this.toString());
  }
}
class PolymorphicKind extends SerialKind {}
class OPEN extends PolymorphicKind {
  constructor() {
    OPEN_instance = null;
    super();
    OPEN_instance = this;
  }
}
class CONTEXTUAL extends SerialKind {
  constructor() {
    CONTEXTUAL_instance = null;
    super();
    CONTEXTUAL_instance = this;
  }
}
class StructureKind extends SerialKind {}
class CLASS extends StructureKind {
  constructor() {
    CLASS_instance = null;
    super();
    CLASS_instance = this;
  }
}
class LIST extends StructureKind {
  constructor() {
    LIST_instance = null;
    super();
    LIST_instance = this;
  }
}
class PrimitiveKind extends SerialKind {}
class BOOLEAN extends PrimitiveKind {
  constructor() {
    BOOLEAN_instance = null;
    super();
    BOOLEAN_instance = this;
  }
}
class INT extends PrimitiveKind {
  constructor() {
    INT_instance = null;
    super();
    INT_instance = this;
  }
}
class STRING extends PrimitiveKind {
  constructor() {
    STRING_instance = null;
    super();
    STRING_instance = this;
  }
}
class ListLikeDescriptor {
  constructor(elementDescriptor) {
    this.ag_1 = elementDescriptor;
    this.bg_1 = 1;
  }
  ff() {
    return LIST_getInstance();
  }
  gf() {
    return this.bg_1;
  }
  hf(index) {
    requireNonNegativeIndex(index, this.ef());
    return index.toString();
  }
  if(index) {
    requireNonNegativeIndex(index, this.ef());
    return this.ag_1;
  }
  equals(other) {
    if (this === other)
      return true;
    if (!(other instanceof ListLikeDescriptor))
      return false;
    if (equals(this.ag_1, other.ag_1) && this.ef() === other.ef())
      return true;
    return false;
  }
  hashCode() {
    return imul(hashCode(this.ag_1), 31) + getStringHashCode(this.ef()) | 0;
  }
  toString() {
    return this.ef() + '(' + toString(this.ag_1) + ')';
  }
}
class ArrayClassDesc extends ListLikeDescriptor {
  ef() {
    return 'kotlin.Array';
  }
}
class AbstractCollectionSerializer {}
class CollectionLikeSerializer extends AbstractCollectionSerializer {
  constructor(elementSerializer) {
    super();
    this.eg_1 = elementSerializer;
  }
}
class ReferenceArraySerializer extends CollectionLikeSerializer {
  constructor(kClass, eSerializer) {
    super(eSerializer);
    this.gg_1 = kClass;
    this.hg_1 = new ArrayClassDesc(eSerializer.vd());
  }
  vd() {
    return this.hg_1;
  }
}
class NullableSerializer {
  constructor(serializer) {
    this.ig_1 = serializer;
    this.jg_1 = new SerialDescriptorForNullable(this.ig_1.vd());
  }
  vd() {
    return this.jg_1;
  }
  equals(other) {
    if (this === other)
      return true;
    if (other == null || !getKClassFromExpression(this).equals(getKClassFromExpression(other)))
      return false;
    if (!(other instanceof NullableSerializer))
      THROW_CCE();
    if (!equals(this.ig_1, other.ig_1))
      return false;
    return true;
  }
  hashCode() {
    return hashCode(this.ig_1);
  }
}
class SerialDescriptorForNullable {
  constructor(original) {
    this.kg_1 = original;
    this.lg_1 = this.kg_1.ef() + '?';
    this.mg_1 = cachedSerialNames(this.kg_1);
  }
  ef() {
    return this.lg_1;
  }
  zf() {
    return this.mg_1;
  }
  af() {
    return true;
  }
  equals(other) {
    if (this === other)
      return true;
    if (!(other instanceof SerialDescriptorForNullable))
      return false;
    if (!equals(this.kg_1, other.kg_1))
      return false;
    return true;
  }
  toString() {
    return toString(this.kg_1) + '?';
  }
  hashCode() {
    return imul(hashCode(this.kg_1), 31);
  }
  ff() {
    return this.kg_1.ff();
  }
  gf() {
    return this.kg_1.gf();
  }
  hf(index) {
    return this.kg_1.hf(index);
  }
  if(index) {
    return this.kg_1.if(index);
  }
}
class PluginGeneratedSerialDescriptor {
  constructor(serialName, generatedSerializer, elementsCount) {
    generatedSerializer = generatedSerializer === VOID ? null : generatedSerializer;
    this.ng_1 = serialName;
    this.og_1 = generatedSerializer;
    this.pg_1 = elementsCount;
    this.qg_1 = -1;
    var tmp = this;
    var tmp_0 = 0;
    var tmp_1 = this.pg_1;
    // Inline function 'kotlin.arrayOfNulls' call
    var tmp_2 = Array(tmp_1);
    while (tmp_0 < tmp_1) {
      tmp_2[tmp_0] = '[UNINITIALIZED]';
      tmp_0 = tmp_0 + 1 | 0;
    }
    tmp.rg_1 = tmp_2;
    var tmp_3 = this;
    // Inline function 'kotlin.arrayOfNulls' call
    var size = this.pg_1;
    tmp_3.sg_1 = Array(size);
    this.tg_1 = null;
    this.ug_1 = booleanArray(this.pg_1);
    this.vg_1 = emptyMap();
    var tmp_4 = this;
    var tmp_5 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    tmp_4.wg_1 = lazy(tmp_5, PluginGeneratedSerialDescriptor$childSerializers$delegate$lambda(this));
    var tmp_6 = this;
    var tmp_7 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    tmp_6.xg_1 = lazy(tmp_7, PluginGeneratedSerialDescriptor$typeParameterDescriptors$delegate$lambda(this));
    var tmp_8 = this;
    var tmp_9 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    tmp_8.yg_1 = lazy(tmp_9, PluginGeneratedSerialDescriptor$_hashCode$delegate$lambda(this));
  }
  ef() {
    return this.ng_1;
  }
  gf() {
    return this.pg_1;
  }
  ff() {
    return CLASS_getInstance();
  }
  zf() {
    return this.vg_1.n1();
  }
  bh() {
    var tmp0 = this.xg_1;
    var tmp = KProperty1;
    // Inline function 'kotlin.getValue' call
    getPropertyCallableRef('typeParameterDescriptors', 1, tmp, PluginGeneratedSerialDescriptor$_get_typeParameterDescriptors_$ref_jk3pka(), null);
    return tmp0.i1();
  }
  ch(name, isOptional) {
    this.qg_1 = this.qg_1 + 1 | 0;
    this.rg_1[this.qg_1] = name;
    this.ug_1[this.qg_1] = isOptional;
    this.sg_1[this.qg_1] = null;
    if (this.qg_1 === (this.pg_1 - 1 | 0)) {
      this.vg_1 = buildIndices(this);
    }
  }
  if(index) {
    return getChecked(_get_childSerializers__7vnyfa(this), index).vd();
  }
  hf(index) {
    return getChecked(this.rg_1, index);
  }
  equals(other) {
    var tmp$ret$0;
    $l$block_5: {
      // Inline function 'kotlinx.serialization.internal.equalsImpl' call
      if (this === other) {
        tmp$ret$0 = true;
        break $l$block_5;
      }
      if (!(other instanceof PluginGeneratedSerialDescriptor)) {
        tmp$ret$0 = false;
        break $l$block_5;
      }
      if (!(this.ef() === other.ef())) {
        tmp$ret$0 = false;
        break $l$block_5;
      }
      if (!contentEquals(this.bh(), other.bh())) {
        tmp$ret$0 = false;
        break $l$block_5;
      }
      if (!(this.gf() === other.gf())) {
        tmp$ret$0 = false;
        break $l$block_5;
      }
      var inductionVariable = 0;
      var last = this.gf();
      if (inductionVariable < last)
        do {
          var index = inductionVariable;
          inductionVariable = inductionVariable + 1 | 0;
          if (!(this.if(index).ef() === other.if(index).ef())) {
            tmp$ret$0 = false;
            break $l$block_5;
          }
          if (!equals(this.if(index).ff(), other.if(index).ff())) {
            tmp$ret$0 = false;
            break $l$block_5;
          }
        }
         while (inductionVariable < last);
      tmp$ret$0 = true;
    }
    return tmp$ret$0;
  }
  hashCode() {
    return _get__hashCode__tgwhef_0(this);
  }
  toString() {
    return toStringImpl(this);
  }
}
class GeneratedSerializer {}
function typeParametersSerializers() {
  return get_EMPTY_SERIALIZER_ARRAY();
}
class StringSerializer {
  constructor() {
    StringSerializer_instance = this;
    this.dh_1 = new PrimitiveSerialDescriptor('kotlin.String', STRING_getInstance());
  }
  vd() {
    return this.dh_1;
  }
}
class IntSerializer {
  constructor() {
    IntSerializer_instance = this;
    this.eh_1 = new PrimitiveSerialDescriptor('kotlin.Int', INT_getInstance());
  }
  vd() {
    return this.eh_1;
  }
}
class PrimitiveSerialDescriptor {
  constructor(serialName, kind) {
    this.fh_1 = serialName;
    this.gh_1 = kind;
  }
  ef() {
    return this.fh_1;
  }
  ff() {
    return this.gh_1;
  }
  gf() {
    return 0;
  }
  hf(index) {
    error(this);
  }
  if(index) {
    error(this);
  }
  toString() {
    return 'PrimitiveDescriptor(' + this.fh_1 + ')';
  }
  equals(other) {
    if (this === other)
      return true;
    if (!(other instanceof PrimitiveSerialDescriptor))
      return false;
    if (this.fh_1 === other.fh_1 && equals(this.gh_1, other.gh_1))
      return true;
    return false;
  }
  hashCode() {
    return getStringHashCode(this.fh_1) + imul(31, this.gh_1.hashCode()) | 0;
  }
}
class BooleanSerializer {
  constructor() {
    BooleanSerializer_instance = this;
    this.hh_1 = new PrimitiveSerialDescriptor('kotlin.Boolean', BOOLEAN_getInstance());
  }
  vd() {
    return this.hh_1;
  }
}
class SerializableWith {}
//endregion
function PolymorphicSerializer$descriptor$delegate$lambda$lambda(this$0) {
  return ($this$buildSerialDescriptor) => {
    $this$buildSerialDescriptor.ee('type', serializer(StringCompanionObject_instance).vd());
    $this$buildSerialDescriptor.ee('value', buildSerialDescriptor('kotlinx.serialization.Polymorphic<' + this$0.fe_1.r9() + '>', CONTEXTUAL_getInstance(), []));
    $this$buildSerialDescriptor.yd_1 = this$0.ge_1;
    return Unit_instance;
  };
}
function PolymorphicSerializer$descriptor$delegate$lambda(this$0) {
  return () => {
    var tmp = OPEN_getInstance();
    return withContext(buildSerialDescriptor('kotlinx.serialization.Polymorphic', tmp, [], PolymorphicSerializer$descriptor$delegate$lambda$lambda(this$0)), this$0.fe_1);
  };
}
function PolymorphicSerializer$_get_descriptor_$ref_8tw9if() {
  return constructCallableReference((p0) => p0.vd(), 1, 0, 1);
}
function init_kotlinx_serialization_SerializationException(_this__u8e3s4) {
  captureStack(_this__u8e3s4, _this__u8e3s4.oe_1);
}
function get_nullable(_this__u8e3s4) {
  var tmp;
  if (_this__u8e3s4.vd().af()) {
    tmp = isInterface(_this__u8e3s4, KSerializer) ? _this__u8e3s4 : THROW_CCE();
  } else {
    tmp = new NullableSerializer(_this__u8e3s4);
  }
  return tmp;
}
function serializer(_this__u8e3s4) {
  return StringSerializer_getInstance();
}
function withContext(_this__u8e3s4, context) {
  return new ContextDescriptor(_this__u8e3s4, context);
}
function get_elementDescriptors(_this__u8e3s4) {
  // Inline function 'kotlin.collections.Iterable' call
  return new elementDescriptors$$inlined$Iterable$1(_this__u8e3s4);
}
function buildSerialDescriptor(serialName, kind, typeParameters, builder) {
  var tmp;
  if (builder === VOID) {
    tmp = buildSerialDescriptor$lambda;
  } else {
    tmp = builder;
  }
  builder = tmp;
  // Inline function 'kotlin.text.isNotBlank' call
  // Inline function 'kotlin.require' call
  if (!!isBlank(serialName)) {
    var message = 'Blank serial names are prohibited';
    throw IllegalArgumentException.g3(toString(message));
  }
  // Inline function 'kotlin.require' call
  if (!!equals(kind, CLASS_getInstance())) {
    var message_0 = "For StructureKind.CLASS please use 'buildClassSerialDescriptor' instead";
    throw IllegalArgumentException.g3(toString(message_0));
  }
  var sdBuilder = new ClassSerialDescriptorBuilder(serialName);
  builder(sdBuilder);
  return new SerialDescriptorImpl(serialName, kind, sdBuilder.zd_1.n(), toList(typeParameters), sdBuilder);
}
function _get__hashCode__tgwhef($this) {
  var tmp0 = $this.yf_1;
  var tmp = KProperty1;
  // Inline function 'kotlin.getValue' call
  getPropertyCallableRef('_hashCode', 1, tmp, SerialDescriptorImpl$_get__hashCode_$ref_2v7wzp(), null);
  return tmp0.i1();
}
function SerialDescriptorImpl$_hashCode$delegate$lambda(this$0) {
  return () => hashCodeImpl(this$0, this$0.xf_1);
}
function SerialDescriptorImpl$_get__hashCode_$ref_2v7wzp() {
  return constructCallableReference((p0) => _get__hashCode__tgwhef(p0), 1, 0, 2);
}
function buildSerialDescriptor$lambda(_this__u8e3s4) {
  return Unit_instance;
}
var OPEN_instance;
function OPEN_getInstance() {
  if (OPEN_instance === VOID)
    new OPEN();
  return OPEN_instance;
}
var CONTEXTUAL_instance;
function CONTEXTUAL_getInstance() {
  if (CONTEXTUAL_instance === VOID)
    new CONTEXTUAL();
  return CONTEXTUAL_instance;
}
var CLASS_instance;
function CLASS_getInstance() {
  if (CLASS_instance === VOID)
    new CLASS();
  return CLASS_instance;
}
var LIST_instance;
function LIST_getInstance() {
  if (LIST_instance === VOID)
    new LIST();
  return LIST_instance;
}
var BOOLEAN_instance;
function BOOLEAN_getInstance() {
  if (BOOLEAN_instance === VOID)
    new BOOLEAN();
  return BOOLEAN_instance;
}
var INT_instance;
function INT_getInstance() {
  if (INT_instance === VOID)
    new INT();
  return INT_instance;
}
var STRING_instance;
function STRING_getInstance() {
  if (STRING_instance === VOID)
    new STRING();
  return STRING_instance;
}
function requireNonNegativeIndex(index, name) {
  if (index < 0)
    throw IndexOutOfBoundsException.k9('Illegal index ' + index + ', ' + name + ' expects only non-negative indices');
}
function get_EMPTY_DESCRIPTOR_ARRAY() {
  _init_properties_Platform_common_kt__3qzecs();
  return EMPTY_DESCRIPTOR_ARRAY;
}
var EMPTY_DESCRIPTOR_ARRAY;
function compactArray(_this__u8e3s4) {
  _init_properties_Platform_common_kt__3qzecs();
  // Inline function 'kotlin.takeUnless' call
  var tmp;
  // Inline function 'kotlin.collections.isNullOrEmpty' call
  if (!(_this__u8e3s4 == null || _this__u8e3s4.y())) {
    tmp = _this__u8e3s4;
  } else {
    tmp = null;
  }
  var tmp0_safe_receiver = tmp;
  var tmp_0;
  if (tmp0_safe_receiver == null) {
    tmp_0 = null;
  } else {
    // Inline function 'kotlin.collections.toTypedArray' call
    tmp_0 = copyToArray(tmp0_safe_receiver);
  }
  var tmp1_elvis_lhs = tmp_0;
  return tmp1_elvis_lhs == null ? get_EMPTY_DESCRIPTOR_ARRAY() : tmp1_elvis_lhs;
}
function cachedSerialNames(_this__u8e3s4) {
  _init_properties_Platform_common_kt__3qzecs();
  if (isInterface(_this__u8e3s4, CachedNames))
    return _this__u8e3s4.zf();
  var result = HashSet.r(_this__u8e3s4.gf());
  var inductionVariable = 0;
  var last = _this__u8e3s4.gf();
  if (inductionVariable < last)
    do {
      var i = inductionVariable;
      inductionVariable = inductionVariable + 1 | 0;
      // Inline function 'kotlin.collections.plusAssign' call
      var element = _this__u8e3s4.hf(i);
      result.j(element);
    }
     while (inductionVariable < last);
  return result;
}
var properties_initialized_Platform_common_kt_i7q4ty;
function _init_properties_Platform_common_kt__3qzecs() {
  if (!properties_initialized_Platform_common_kt_i7q4ty) {
    properties_initialized_Platform_common_kt_i7q4ty = true;
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    EMPTY_DESCRIPTOR_ARRAY = [];
  }
}
function throwMissingFieldException(seen, goldenMask, descriptor) {
  // Inline function 'kotlin.collections.mutableListOf' call
  var missingFields = ArrayList.i();
  var missingFieldsBits = goldenMask & ~seen;
  var inductionVariable = 0;
  if (inductionVariable < 32)
    do {
      var i = inductionVariable;
      inductionVariable = inductionVariable + 1 | 0;
      if (!((missingFieldsBits & 1) === 0)) {
        // Inline function 'kotlin.collections.plusAssign' call
        var element = descriptor.hf(i);
        missingFields.j(element);
      }
      missingFieldsBits = missingFieldsBits >>> 1 | 0;
    }
     while (inductionVariable < 32);
  throw MissingFieldException.ze(missingFields, descriptor.ef());
}
function _get_childSerializers__7vnyfa($this) {
  var tmp0 = $this.wg_1;
  var tmp = KProperty1;
  // Inline function 'kotlin.getValue' call
  getPropertyCallableRef('childSerializers', 1, tmp, PluginGeneratedSerialDescriptor$_get_childSerializers_$ref_e7suca(), null);
  return tmp0.i1();
}
function _get__hashCode__tgwhef_0($this) {
  var tmp0 = $this.yg_1;
  var tmp = KProperty1;
  // Inline function 'kotlin.getValue' call
  getPropertyCallableRef('_hashCode', 1, tmp, PluginGeneratedSerialDescriptor$_get__hashCode_$ref_cmj4vz(), null);
  return tmp0.i1();
}
function buildIndices($this) {
  var indices = HashMap.k5();
  var inductionVariable = 0;
  var last = $this.rg_1.length - 1 | 0;
  if (inductionVariable <= last)
    do {
      var i = inductionVariable;
      inductionVariable = inductionVariable + 1 | 0;
      // Inline function 'kotlin.collections.set' call
      var key = $this.rg_1[i];
      indices.q2(key, i);
    }
     while (inductionVariable <= last);
  return indices;
}
function PluginGeneratedSerialDescriptor$childSerializers$delegate$lambda(this$0) {
  return () => {
    var tmp0_safe_receiver = this$0.og_1;
    var tmp1_elvis_lhs = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.zg();
    return tmp1_elvis_lhs == null ? get_EMPTY_SERIALIZER_ARRAY() : tmp1_elvis_lhs;
  };
}
function PluginGeneratedSerialDescriptor$_get_childSerializers_$ref_e7suca() {
  return constructCallableReference((p0) => _get_childSerializers__7vnyfa(p0), 1, 0, 3);
}
function PluginGeneratedSerialDescriptor$typeParameterDescriptors$delegate$lambda(this$0) {
  return () => {
    var tmp0_safe_receiver = this$0.og_1;
    var tmp1_safe_receiver = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.ah();
    var tmp;
    if (tmp1_safe_receiver == null) {
      tmp = null;
    } else {
      // Inline function 'kotlin.collections.map' call
      // Inline function 'kotlin.collections.mapTo' call
      var destination = ArrayList.o(tmp1_safe_receiver.length);
      var inductionVariable = 0;
      var last = tmp1_safe_receiver.length;
      while (inductionVariable < last) {
        var item = tmp1_safe_receiver[inductionVariable];
        inductionVariable = inductionVariable + 1 | 0;
        var tmp$ret$2 = item.vd();
        destination.j(tmp$ret$2);
      }
      tmp = destination;
    }
    return compactArray(tmp);
  };
}
function PluginGeneratedSerialDescriptor$_get_typeParameterDescriptors_$ref_jk3pka() {
  return constructCallableReference((p0) => p0.bh(), 1, 0, 4);
}
function PluginGeneratedSerialDescriptor$_hashCode$delegate$lambda(this$0) {
  return () => hashCodeImpl(this$0, this$0.bh());
}
function PluginGeneratedSerialDescriptor$_get__hashCode_$ref_cmj4vz() {
  return constructCallableReference((p0) => _get__hashCode__tgwhef_0(p0), 1, 0, 5);
}
function hashCodeImpl(_this__u8e3s4, typeParams) {
  var result = getStringHashCode(_this__u8e3s4.ef());
  result = imul(31, result) + contentHashCode(typeParams) | 0;
  var elementDescriptors = get_elementDescriptors(_this__u8e3s4);
  // Inline function 'kotlinx.serialization.internal.elementsHashCodeBy' call
  // Inline function 'kotlin.collections.fold' call
  var accumulator = 1;
  var _iterator__ex2g4s = elementDescriptors.k();
  while (_iterator__ex2g4s.l()) {
    var element = _iterator__ex2g4s.m();
    var hash = accumulator;
    var tmp = imul(31, hash);
    // Inline function 'kotlin.hashCode' call
    var tmp0_safe_receiver = element.ef();
    var tmp1_elvis_lhs = tmp0_safe_receiver == null ? null : hashCode(tmp0_safe_receiver);
    accumulator = tmp + (tmp1_elvis_lhs == null ? 0 : tmp1_elvis_lhs) | 0;
  }
  var namesHash = accumulator;
  // Inline function 'kotlinx.serialization.internal.elementsHashCodeBy' call
  // Inline function 'kotlin.collections.fold' call
  var accumulator_0 = 1;
  var _iterator__ex2g4s_0 = elementDescriptors.k();
  while (_iterator__ex2g4s_0.l()) {
    var element_0 = _iterator__ex2g4s_0.m();
    var hash_0 = accumulator_0;
    var tmp_0 = imul(31, hash_0);
    // Inline function 'kotlin.hashCode' call
    var tmp0_safe_receiver_0 = element_0.ff();
    var tmp1_elvis_lhs_0 = tmp0_safe_receiver_0 == null ? null : hashCode(tmp0_safe_receiver_0);
    accumulator_0 = tmp_0 + (tmp1_elvis_lhs_0 == null ? 0 : tmp1_elvis_lhs_0) | 0;
  }
  var kindHash = accumulator_0;
  result = imul(31, result) + namesHash | 0;
  result = imul(31, result) + kindHash | 0;
  return result;
}
function toStringImpl(_this__u8e3s4) {
  var tmp = until(0, _this__u8e3s4.gf());
  var tmp_0 = _this__u8e3s4.ef() + '(';
  return joinToString(tmp, ', ', tmp_0, ')', VOID, VOID, toStringImpl$lambda(_this__u8e3s4));
}
function toStringImpl$lambda($this_toStringImpl) {
  return (i) => $this_toStringImpl.hf(i) + ': ' + $this_toStringImpl.if(i).ef();
}
function get_EMPTY_SERIALIZER_ARRAY() {
  _init_properties_PluginHelperInterfaces_kt__xgvzfp();
  return EMPTY_SERIALIZER_ARRAY;
}
var EMPTY_SERIALIZER_ARRAY;
var properties_initialized_PluginHelperInterfaces_kt_ap8in1;
function _init_properties_PluginHelperInterfaces_kt__xgvzfp() {
  if (!properties_initialized_PluginHelperInterfaces_kt_ap8in1) {
    properties_initialized_PluginHelperInterfaces_kt_ap8in1 = true;
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    EMPTY_SERIALIZER_ARRAY = [];
  }
}
var StringSerializer_instance;
function StringSerializer_getInstance() {
  if (StringSerializer_instance === VOID)
    new StringSerializer();
  return StringSerializer_instance;
}
var IntSerializer_instance;
function IntSerializer_getInstance() {
  if (IntSerializer_instance === VOID)
    new IntSerializer();
  return IntSerializer_instance;
}
function error($this) {
  throw IllegalStateException.u2('Primitive descriptor ' + $this.fh_1 + ' does not have elements');
}
var BooleanSerializer_instance;
function BooleanSerializer_getInstance() {
  if (BooleanSerializer_instance === VOID)
    new BooleanSerializer();
  return BooleanSerializer_instance;
}
function getChecked(_this__u8e3s4, index) {
  if (!(0 <= index ? index <= (_this__u8e3s4.length - 1 | 0) : false))
    throw IndexOutOfBoundsException.k9('Index ' + index + ' out of bounds ' + get_indices(_this__u8e3s4).toString());
  return _this__u8e3s4[index];
}
//region block: post-declaration
initMetadataForInterface(KSerializer, 'KSerializer');
initMetadataForClass(AbstractPolymorphicSerializer, 'AbstractPolymorphicSerializer', VOID, VOID, [KSerializer]);
initMetadataForClass(PolymorphicSerializer, 'PolymorphicSerializer');
initMetadataForClass(SerializationException, 'SerializationException', SerializationException.pe);
initMetadataForClass(MissingFieldException, 'MissingFieldException');
initMetadataForInterface(SerialDescriptor, 'SerialDescriptor');
initMetadataForClass(ContextDescriptor, 'ContextDescriptor', VOID, VOID, [SerialDescriptor]);
initMetadataForClass(elementDescriptors$1);
initMetadataForClass(elementDescriptors$$inlined$Iterable$1);
initMetadataForClass(ClassSerialDescriptorBuilder, 'ClassSerialDescriptorBuilder');
initMetadataForInterface(CachedNames, 'CachedNames');
protoOf(SerialDescriptorImpl).af = get_isNullable;
initMetadataForClass(SerialDescriptorImpl, 'SerialDescriptorImpl', VOID, VOID, [SerialDescriptor, CachedNames]);
initMetadataForClass(SerialKind, 'SerialKind');
initMetadataForClass(PolymorphicKind, 'PolymorphicKind');
initMetadataForObject(OPEN, 'OPEN');
initMetadataForObject(CONTEXTUAL, 'CONTEXTUAL');
initMetadataForClass(StructureKind, 'StructureKind');
initMetadataForObject(CLASS, 'CLASS');
initMetadataForObject(LIST, 'LIST');
initMetadataForClass(PrimitiveKind, 'PrimitiveKind');
initMetadataForObject(BOOLEAN, 'BOOLEAN');
initMetadataForObject(INT, 'INT');
initMetadataForObject(STRING, 'STRING');
protoOf(ListLikeDescriptor).af = get_isNullable;
initMetadataForClass(ListLikeDescriptor, 'ListLikeDescriptor', VOID, VOID, [SerialDescriptor]);
initMetadataForClass(ArrayClassDesc, 'ArrayClassDesc');
initMetadataForClass(AbstractCollectionSerializer, 'AbstractCollectionSerializer', VOID, VOID, [KSerializer]);
initMetadataForClass(CollectionLikeSerializer, 'CollectionLikeSerializer');
initMetadataForClass(ReferenceArraySerializer, 'ReferenceArraySerializer');
initMetadataForClass(NullableSerializer, 'NullableSerializer', VOID, VOID, [KSerializer]);
initMetadataForClass(SerialDescriptorForNullable, 'SerialDescriptorForNullable', VOID, VOID, [SerialDescriptor, CachedNames]);
protoOf(PluginGeneratedSerialDescriptor).af = get_isNullable;
initMetadataForClass(PluginGeneratedSerialDescriptor, 'PluginGeneratedSerialDescriptor', VOID, VOID, [SerialDescriptor, CachedNames]);
initMetadataForInterface(GeneratedSerializer, 'GeneratedSerializer', VOID, VOID, [KSerializer]);
initMetadataForObject(StringSerializer, 'StringSerializer', VOID, VOID, [KSerializer]);
initMetadataForObject(IntSerializer, 'IntSerializer', VOID, VOID, [KSerializer]);
protoOf(PrimitiveSerialDescriptor).af = get_isNullable;
initMetadataForClass(PrimitiveSerialDescriptor, 'PrimitiveSerialDescriptor', VOID, VOID, [SerialDescriptor]);
initMetadataForObject(BooleanSerializer, 'BooleanSerializer', VOID, VOID, [KSerializer]);
initMetadataForClass(SerializableWith, 'SerializableWith', VOID, VOID, VOID, VOID, 0);
//endregion
//region block: exports
export {
  BooleanSerializer_getInstance as BooleanSerializer_getInstance22aja3hu17sxf,
  IntSerializer_getInstance as IntSerializer_getInstance3jyhns9ue706g,
  StringSerializer_getInstance as StringSerializer_getInstance1p9zda6r6kb23,
  get_nullable as get_nullable197rfua9r7fsz,
  typeParametersSerializers as typeParametersSerializers2likxjr48tr7y,
  GeneratedSerializer as GeneratedSerializer1f7t7hssdd2ws,
  PluginGeneratedSerialDescriptor as PluginGeneratedSerialDescriptorqdzeg5asqhfg,
  ReferenceArraySerializer as ReferenceArraySerializer3juj1vqolxkrs,
  throwMissingFieldException as throwMissingFieldException2cmke0v3ynf14,
  PolymorphicSerializer as PolymorphicSerializer3p3fzpdobi8hh,
  SerializableWith as SerializableWithd2dap36updxd,
};
//endregion

//# sourceMappingURL=kotlinx-serialization-kotlinx-serialization-core.mjs.map

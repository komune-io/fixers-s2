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
  static pf() {
    return createThis(this);
  }
}
class PolymorphicSerializer extends AbstractPolymorphicSerializer {
  constructor(baseClass) {
    return new.target.of(baseClass);
  }
  static of(baseClass) {
    var $this = this.pf();
    $this.lf_1 = baseClass;
    $this.mf_1 = emptyList();
    var tmp = $this;
    var tmp_0 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    tmp.nf_1 = lazy(tmp_0, PolymorphicSerializer$descriptor$delegate$lambda($this));
    return $this;
  }
  static qf(baseClass, classAnnotations) {
    var $this = this.of(baseClass);
    $this.mf_1 = asList(classAnnotations);
    return $this;
  }
  bf() {
    var tmp0 = this.nf_1;
    var tmp = KProperty1;
    // Inline function 'kotlin.getValue' call
    getPropertyCallableRef('descriptor', 1, tmp, PolymorphicSerializer$_get_descriptor_$ref_8tw9if(), null);
    return tmp0.i1();
  }
  toString() {
    return 'kotlinx.serialization.PolymorphicSerializer(baseClass: ' + toString(this.lf_1) + ')';
  }
}
class SerializationException extends IllegalArgumentException {
  static vf() {
    var $this = this.ba();
    init_kotlinx_serialization_SerializationException($this);
    return $this;
  }
  static wf(message, cause) {
    var $this = this.ca(message, cause);
    init_kotlinx_serialization_SerializationException($this);
    return $this;
  }
}
class MissingFieldException extends SerializationException {
  constructor(message, cause, missingFields, serialName) {
    return new.target.eg(message, cause, missingFields, serialName);
  }
  static eg(message, cause, missingFields, serialName) {
    var $this = this.wf(message, cause);
    captureStack($this, $this.dg_1);
    $this.bg_1 = missingFields;
    $this.cg_1 = serialName;
    return $this;
  }
  static fg(missingFields, serialName) {
    var tmp0_message = missingFields.n() === 1 ? "Field '" + missingFields.s(0) + "' is required for type with serial name '" + serialName + "', but it was missing" : 'Fields ' + toString(missingFields) + " are required for type with serial name '" + serialName + "', but they were missing";
    return this.eg(tmp0_message, null, missingFields, serialName);
  }
}
class SerialDescriptor {}
function get_isNullable() {
  return false;
}
class ContextDescriptor {
  constructor(original, kClass) {
    this.hg_1 = original;
    this.ig_1 = kClass;
    this.jg_1 = this.hg_1.kg() + '<' + this.ig_1.wa() + '>';
  }
  kg() {
    return this.jg_1;
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
    return equals(this.hg_1, another.hg_1) && another.ig_1.equals(this.ig_1);
  }
  hashCode() {
    var result = this.ig_1.hashCode();
    result = imul(31, result) + getStringHashCode(this.jg_1) | 0;
    return result;
  }
  toString() {
    return 'ContextDescriptor(kClass: ' + toString(this.ig_1) + ', original: ' + toString(this.hg_1) + ')';
  }
  lg() {
    return this.hg_1.lg();
  }
  gg() {
    return this.hg_1.gg();
  }
  mg() {
    return this.hg_1.mg();
  }
  ng(index) {
    return this.hg_1.ng(index);
  }
  og(index) {
    return this.hg_1.og(index);
  }
}
class elementDescriptors$1 {
  constructor($this_elementDescriptors) {
    this.qg_1 = $this_elementDescriptors;
    this.pg_1 = $this_elementDescriptors.mg();
  }
  l() {
    return this.pg_1 > 0;
  }
  m() {
    var tmp = this.qg_1.mg();
    var _unary__edvuaz = this.pg_1;
    this.pg_1 = _unary__edvuaz - 1 | 0;
    return this.qg_1.og(tmp - _unary__edvuaz | 0);
  }
}
class elementDescriptors$$inlined$Iterable$1 {
  constructor($this_elementDescriptors) {
    this.rg_1 = $this_elementDescriptors;
  }
  k() {
    return new elementDescriptors$1(this.rg_1);
  }
}
class ClassSerialDescriptorBuilder {
  constructor(serialName) {
    this.cf_1 = serialName;
    this.df_1 = false;
    this.ef_1 = emptyList();
    this.ff_1 = ArrayList.i();
    this.gf_1 = HashSet.z6();
    this.hf_1 = ArrayList.i();
    this.if_1 = ArrayList.i();
    this.jf_1 = ArrayList.i();
  }
  sg(elementName, descriptor, annotations, isOptional) {
    // Inline function 'kotlin.require' call
    if (!this.gf_1.j(elementName)) {
      var message = "Element with name '" + elementName + "' is already registered in " + this.cf_1;
      throw IllegalArgumentException.g3(toString(message));
    }
    // Inline function 'kotlin.collections.plusAssign' call
    this.ff_1.j(elementName);
    // Inline function 'kotlin.collections.plusAssign' call
    this.hf_1.j(descriptor);
    // Inline function 'kotlin.collections.plusAssign' call
    this.if_1.j(annotations);
    // Inline function 'kotlin.collections.plusAssign' call
    this.jf_1.j(isOptional);
  }
  kf(elementName, descriptor, annotations, isOptional, $super) {
    annotations = annotations === VOID ? emptyList() : annotations;
    isOptional = isOptional === VOID ? false : isOptional;
    var tmp;
    if ($super === VOID) {
      this.sg(elementName, descriptor, annotations, isOptional);
      tmp = Unit_instance;
    } else {
      tmp = $super.sg.call(this, elementName, descriptor, annotations, isOptional);
    }
    return tmp;
  }
}
class CachedNames {}
class SerialDescriptorImpl {
  constructor(serialName, kind, elementsCount, typeParameters, builder) {
    this.tg_1 = serialName;
    this.ug_1 = kind;
    this.vg_1 = elementsCount;
    this.wg_1 = builder.ef_1;
    this.xg_1 = toHashSet(builder.ff_1);
    var tmp = this;
    // Inline function 'kotlin.collections.toTypedArray' call
    var this_0 = builder.ff_1;
    tmp.yg_1 = copyToArray(this_0);
    this.zg_1 = compactArray(builder.hf_1);
    var tmp_0 = this;
    // Inline function 'kotlin.collections.toTypedArray' call
    var this_1 = builder.if_1;
    tmp_0.ah_1 = copyToArray(this_1);
    this.bh_1 = toBooleanArray(builder.jf_1);
    var tmp_1 = this;
    // Inline function 'kotlin.collections.map' call
    var this_2 = withIndex(this.yg_1);
    // Inline function 'kotlin.collections.mapTo' call
    var destination = ArrayList.o(collectionSizeOrDefault(this_2, 10));
    var _iterator__ex2g4s = this_2.k();
    while (_iterator__ex2g4s.l()) {
      var item = _iterator__ex2g4s.m();
      var tmp$ret$4 = to(item.zc_1, item.yc_1);
      destination.j(tmp$ret$4);
    }
    tmp_1.ch_1 = toMap(destination);
    this.dh_1 = compactArray(typeParameters);
    var tmp_2 = this;
    tmp_2.eh_1 = lazy_0(SerialDescriptorImpl$_hashCode$delegate$lambda(this));
  }
  kg() {
    return this.tg_1;
  }
  lg() {
    return this.ug_1;
  }
  mg() {
    return this.vg_1;
  }
  fh() {
    return this.xg_1;
  }
  ng(index) {
    return getChecked(this.yg_1, index);
  }
  og(index) {
    return getChecked(this.zg_1, index);
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
      if (!(this.kg() === other.kg())) {
        tmp$ret$0 = false;
        break $l$block_5;
      }
      if (!contentEquals(this.dh_1, other.dh_1)) {
        tmp$ret$0 = false;
        break $l$block_5;
      }
      if (!(this.mg() === other.mg())) {
        tmp$ret$0 = false;
        break $l$block_5;
      }
      var inductionVariable = 0;
      var last = this.mg();
      if (inductionVariable < last)
        do {
          var index = inductionVariable;
          inductionVariable = inductionVariable + 1 | 0;
          if (!(this.og(index).kg() === other.og(index).kg())) {
            tmp$ret$0 = false;
            break $l$block_5;
          }
          if (!equals(this.og(index).lg(), other.og(index).lg())) {
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
    return ensureNotNull(getKClassFromExpression(this).wa());
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
    this.ih_1 = elementDescriptor;
    this.jh_1 = 1;
  }
  lg() {
    return LIST_getInstance();
  }
  mg() {
    return this.jh_1;
  }
  ng(index) {
    requireNonNegativeIndex(index, this.kg());
    return index.toString();
  }
  og(index) {
    requireNonNegativeIndex(index, this.kg());
    return this.ih_1;
  }
  equals(other) {
    if (this === other)
      return true;
    if (!(other instanceof ListLikeDescriptor))
      return false;
    if (equals(this.ih_1, other.ih_1) && this.kg() === other.kg())
      return true;
    return false;
  }
  hashCode() {
    return imul(hashCode(this.ih_1), 31) + getStringHashCode(this.kg()) | 0;
  }
  toString() {
    return this.kg() + '(' + toString(this.ih_1) + ')';
  }
}
class ArrayClassDesc extends ListLikeDescriptor {
  kg() {
    return 'kotlin.Array';
  }
}
class AbstractCollectionSerializer {}
class CollectionLikeSerializer extends AbstractCollectionSerializer {
  constructor(elementSerializer) {
    super();
    this.nh_1 = elementSerializer;
  }
}
class ReferenceArraySerializer extends CollectionLikeSerializer {
  constructor(kClass, eSerializer) {
    super(eSerializer);
    this.lh_1 = kClass;
    this.mh_1 = new ArrayClassDesc(eSerializer.bf());
  }
  bf() {
    return this.mh_1;
  }
}
class NullableSerializer {
  constructor(serializer) {
    this.oh_1 = serializer;
    this.ph_1 = new SerialDescriptorForNullable(this.oh_1.bf());
  }
  bf() {
    return this.ph_1;
  }
  equals(other) {
    if (this === other)
      return true;
    if (other == null || !getKClassFromExpression(this).equals(getKClassFromExpression(other)))
      return false;
    if (!(other instanceof NullableSerializer))
      THROW_CCE();
    if (!equals(this.oh_1, other.oh_1))
      return false;
    return true;
  }
  hashCode() {
    return hashCode(this.oh_1);
  }
}
class SerialDescriptorForNullable {
  constructor(original) {
    this.qh_1 = original;
    this.rh_1 = this.qh_1.kg() + '?';
    this.sh_1 = cachedSerialNames(this.qh_1);
  }
  kg() {
    return this.rh_1;
  }
  fh() {
    return this.sh_1;
  }
  gg() {
    return true;
  }
  equals(other) {
    if (this === other)
      return true;
    if (!(other instanceof SerialDescriptorForNullable))
      return false;
    if (!equals(this.qh_1, other.qh_1))
      return false;
    return true;
  }
  toString() {
    return toString(this.qh_1) + '?';
  }
  hashCode() {
    return imul(hashCode(this.qh_1), 31);
  }
  lg() {
    return this.qh_1.lg();
  }
  mg() {
    return this.qh_1.mg();
  }
  ng(index) {
    return this.qh_1.ng(index);
  }
  og(index) {
    return this.qh_1.og(index);
  }
}
class PluginGeneratedSerialDescriptor {
  constructor(serialName, generatedSerializer, elementsCount) {
    generatedSerializer = generatedSerializer === VOID ? null : generatedSerializer;
    this.th_1 = serialName;
    this.uh_1 = generatedSerializer;
    this.vh_1 = elementsCount;
    this.wh_1 = -1;
    var tmp = this;
    var tmp_0 = 0;
    var tmp_1 = this.vh_1;
    // Inline function 'kotlin.arrayOfNulls' call
    var tmp_2 = Array(tmp_1);
    while (tmp_0 < tmp_1) {
      tmp_2[tmp_0] = '[UNINITIALIZED]';
      tmp_0 = tmp_0 + 1 | 0;
    }
    tmp.xh_1 = tmp_2;
    var tmp_3 = this;
    // Inline function 'kotlin.arrayOfNulls' call
    var size = this.vh_1;
    tmp_3.yh_1 = Array(size);
    this.zh_1 = null;
    this.ai_1 = booleanArray(this.vh_1);
    this.bi_1 = emptyMap();
    var tmp_4 = this;
    var tmp_5 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    tmp_4.ci_1 = lazy(tmp_5, PluginGeneratedSerialDescriptor$childSerializers$delegate$lambda(this));
    var tmp_6 = this;
    var tmp_7 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    tmp_6.di_1 = lazy(tmp_7, PluginGeneratedSerialDescriptor$typeParameterDescriptors$delegate$lambda(this));
    var tmp_8 = this;
    var tmp_9 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    tmp_8.ei_1 = lazy(tmp_9, PluginGeneratedSerialDescriptor$_hashCode$delegate$lambda(this));
  }
  kg() {
    return this.th_1;
  }
  mg() {
    return this.vh_1;
  }
  lg() {
    return CLASS_getInstance();
  }
  fh() {
    return this.bi_1.n1();
  }
  hi() {
    var tmp0 = this.di_1;
    var tmp = KProperty1;
    // Inline function 'kotlin.getValue' call
    getPropertyCallableRef('typeParameterDescriptors', 1, tmp, PluginGeneratedSerialDescriptor$_get_typeParameterDescriptors_$ref_jk3pka(), null);
    return tmp0.i1();
  }
  ii(name, isOptional) {
    this.wh_1 = this.wh_1 + 1 | 0;
    this.xh_1[this.wh_1] = name;
    this.ai_1[this.wh_1] = isOptional;
    this.yh_1[this.wh_1] = null;
    if (this.wh_1 === (this.vh_1 - 1 | 0)) {
      this.bi_1 = buildIndices(this);
    }
  }
  og(index) {
    return getChecked(_get_childSerializers__7vnyfa(this), index).bf();
  }
  ng(index) {
    return getChecked(this.xh_1, index);
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
      if (!(this.kg() === other.kg())) {
        tmp$ret$0 = false;
        break $l$block_5;
      }
      if (!contentEquals(this.hi(), other.hi())) {
        tmp$ret$0 = false;
        break $l$block_5;
      }
      if (!(this.mg() === other.mg())) {
        tmp$ret$0 = false;
        break $l$block_5;
      }
      var inductionVariable = 0;
      var last = this.mg();
      if (inductionVariable < last)
        do {
          var index = inductionVariable;
          inductionVariable = inductionVariable + 1 | 0;
          if (!(this.og(index).kg() === other.og(index).kg())) {
            tmp$ret$0 = false;
            break $l$block_5;
          }
          if (!equals(this.og(index).lg(), other.og(index).lg())) {
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
    this.ji_1 = new PrimitiveSerialDescriptor('kotlin.String', STRING_getInstance());
  }
  bf() {
    return this.ji_1;
  }
}
class BooleanSerializer {
  constructor() {
    BooleanSerializer_instance = this;
    this.ki_1 = new PrimitiveSerialDescriptor('kotlin.Boolean', BOOLEAN_getInstance());
  }
  bf() {
    return this.ki_1;
  }
}
class IntSerializer {
  constructor() {
    IntSerializer_instance = this;
    this.li_1 = new PrimitiveSerialDescriptor('kotlin.Int', INT_getInstance());
  }
  bf() {
    return this.li_1;
  }
}
class PrimitiveSerialDescriptor {
  constructor(serialName, kind) {
    this.mi_1 = serialName;
    this.ni_1 = kind;
  }
  kg() {
    return this.mi_1;
  }
  lg() {
    return this.ni_1;
  }
  mg() {
    return 0;
  }
  ng(index) {
    error(this);
  }
  og(index) {
    error(this);
  }
  toString() {
    return 'PrimitiveDescriptor(' + this.mi_1 + ')';
  }
  equals(other) {
    if (this === other)
      return true;
    if (!(other instanceof PrimitiveSerialDescriptor))
      return false;
    if (this.mi_1 === other.mi_1 && equals(this.ni_1, other.ni_1))
      return true;
    return false;
  }
  hashCode() {
    return getStringHashCode(this.mi_1) + imul(31, this.ni_1.hashCode()) | 0;
  }
}
class SerializableWith {}
//endregion
function PolymorphicSerializer$descriptor$delegate$lambda$lambda(this$0) {
  return ($this$buildSerialDescriptor) => {
    $this$buildSerialDescriptor.kf('type', serializer(StringCompanionObject_instance).bf());
    $this$buildSerialDescriptor.kf('value', buildSerialDescriptor('kotlinx.serialization.Polymorphic<' + this$0.lf_1.wa() + '>', CONTEXTUAL_getInstance(), []));
    $this$buildSerialDescriptor.ef_1 = this$0.mf_1;
    return Unit_instance;
  };
}
function PolymorphicSerializer$descriptor$delegate$lambda(this$0) {
  return () => {
    var tmp = OPEN_getInstance();
    return withContext(buildSerialDescriptor('kotlinx.serialization.Polymorphic', tmp, [], PolymorphicSerializer$descriptor$delegate$lambda$lambda(this$0)), this$0.lf_1);
  };
}
function PolymorphicSerializer$_get_descriptor_$ref_8tw9if() {
  return constructCallableReference((p0) => p0.bf(), 1, 0, 1);
}
function init_kotlinx_serialization_SerializationException(_this__u8e3s4) {
  captureStack(_this__u8e3s4, _this__u8e3s4.uf_1);
}
function get_nullable(_this__u8e3s4) {
  var tmp;
  if (_this__u8e3s4.bf().gg()) {
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
  return new SerialDescriptorImpl(serialName, kind, sdBuilder.ff_1.n(), toList(typeParameters), sdBuilder);
}
function _get__hashCode__tgwhef($this) {
  var tmp0 = $this.eh_1;
  var tmp = KProperty1;
  // Inline function 'kotlin.getValue' call
  getPropertyCallableRef('_hashCode', 1, tmp, SerialDescriptorImpl$_get__hashCode_$ref_2v7wzp(), null);
  return tmp0.i1();
}
function SerialDescriptorImpl$_hashCode$delegate$lambda(this$0) {
  return () => hashCodeImpl(this$0, this$0.dh_1);
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
    throw IndexOutOfBoundsException.ma('Illegal index ' + index + ', ' + name + ' expects only non-negative indices');
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
    return _this__u8e3s4.fh();
  var result = HashSet.r(_this__u8e3s4.mg());
  var inductionVariable = 0;
  var last = _this__u8e3s4.mg();
  if (inductionVariable < last)
    do {
      var i = inductionVariable;
      inductionVariable = inductionVariable + 1 | 0;
      // Inline function 'kotlin.collections.plusAssign' call
      var element = _this__u8e3s4.ng(i);
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
        var element = descriptor.ng(i);
        missingFields.j(element);
      }
      missingFieldsBits = missingFieldsBits >>> 1 | 0;
    }
     while (inductionVariable < 32);
  throw MissingFieldException.fg(missingFields, descriptor.kg());
}
function _get_childSerializers__7vnyfa($this) {
  var tmp0 = $this.ci_1;
  var tmp = KProperty1;
  // Inline function 'kotlin.getValue' call
  getPropertyCallableRef('childSerializers', 1, tmp, PluginGeneratedSerialDescriptor$_get_childSerializers_$ref_e7suca(), null);
  return tmp0.i1();
}
function _get__hashCode__tgwhef_0($this) {
  var tmp0 = $this.ei_1;
  var tmp = KProperty1;
  // Inline function 'kotlin.getValue' call
  getPropertyCallableRef('_hashCode', 1, tmp, PluginGeneratedSerialDescriptor$_get__hashCode_$ref_cmj4vz(), null);
  return tmp0.i1();
}
function buildIndices($this) {
  var indices = HashMap.l5();
  var inductionVariable = 0;
  var last = $this.xh_1.length - 1 | 0;
  if (inductionVariable <= last)
    do {
      var i = inductionVariable;
      inductionVariable = inductionVariable + 1 | 0;
      // Inline function 'kotlin.collections.set' call
      var key = $this.xh_1[i];
      indices.q2(key, i);
    }
     while (inductionVariable <= last);
  return indices;
}
function PluginGeneratedSerialDescriptor$childSerializers$delegate$lambda(this$0) {
  return () => {
    var tmp0_safe_receiver = this$0.uh_1;
    var tmp1_elvis_lhs = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.fi();
    return tmp1_elvis_lhs == null ? get_EMPTY_SERIALIZER_ARRAY() : tmp1_elvis_lhs;
  };
}
function PluginGeneratedSerialDescriptor$_get_childSerializers_$ref_e7suca() {
  return constructCallableReference((p0) => _get_childSerializers__7vnyfa(p0), 1, 0, 3);
}
function PluginGeneratedSerialDescriptor$typeParameterDescriptors$delegate$lambda(this$0) {
  return () => {
    var tmp0_safe_receiver = this$0.uh_1;
    var tmp1_safe_receiver = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.gi();
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
        var tmp$ret$2 = item.bf();
        destination.j(tmp$ret$2);
      }
      tmp = destination;
    }
    return compactArray(tmp);
  };
}
function PluginGeneratedSerialDescriptor$_get_typeParameterDescriptors_$ref_jk3pka() {
  return constructCallableReference((p0) => p0.hi(), 1, 0, 4);
}
function PluginGeneratedSerialDescriptor$_hashCode$delegate$lambda(this$0) {
  return () => hashCodeImpl(this$0, this$0.hi());
}
function PluginGeneratedSerialDescriptor$_get__hashCode_$ref_cmj4vz() {
  return constructCallableReference((p0) => _get__hashCode__tgwhef_0(p0), 1, 0, 5);
}
function hashCodeImpl(_this__u8e3s4, typeParams) {
  var result = getStringHashCode(_this__u8e3s4.kg());
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
    var tmp0_safe_receiver = element.kg();
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
    var tmp0_safe_receiver_0 = element_0.lg();
    var tmp1_elvis_lhs_0 = tmp0_safe_receiver_0 == null ? null : hashCode(tmp0_safe_receiver_0);
    accumulator_0 = tmp_0 + (tmp1_elvis_lhs_0 == null ? 0 : tmp1_elvis_lhs_0) | 0;
  }
  var kindHash = accumulator_0;
  result = imul(31, result) + namesHash | 0;
  result = imul(31, result) + kindHash | 0;
  return result;
}
function toStringImpl(_this__u8e3s4) {
  var tmp = until(0, _this__u8e3s4.mg());
  var tmp_0 = _this__u8e3s4.kg() + '(';
  return joinToString(tmp, ', ', tmp_0, ')', VOID, VOID, toStringImpl$lambda(_this__u8e3s4));
}
function toStringImpl$lambda($this_toStringImpl) {
  return (i) => $this_toStringImpl.ng(i) + ': ' + $this_toStringImpl.og(i).kg();
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
var BooleanSerializer_instance;
function BooleanSerializer_getInstance() {
  if (BooleanSerializer_instance === VOID)
    new BooleanSerializer();
  return BooleanSerializer_instance;
}
var IntSerializer_instance;
function IntSerializer_getInstance() {
  if (IntSerializer_instance === VOID)
    new IntSerializer();
  return IntSerializer_instance;
}
function error($this) {
  throw IllegalStateException.u2('Primitive descriptor ' + $this.mi_1 + ' does not have elements');
}
function getChecked(_this__u8e3s4, index) {
  if (!(0 <= index ? index <= (_this__u8e3s4.length - 1 | 0) : false))
    throw IndexOutOfBoundsException.ma('Index ' + index + ' out of bounds ' + get_indices(_this__u8e3s4).toString());
  return _this__u8e3s4[index];
}
//region block: post-declaration
initMetadataForInterface(KSerializer, 'KSerializer');
initMetadataForClass(AbstractPolymorphicSerializer, 'AbstractPolymorphicSerializer', VOID, VOID, [KSerializer]);
initMetadataForClass(PolymorphicSerializer, 'PolymorphicSerializer');
initMetadataForClass(SerializationException, 'SerializationException', SerializationException.vf);
initMetadataForClass(MissingFieldException, 'MissingFieldException');
initMetadataForInterface(SerialDescriptor, 'SerialDescriptor');
initMetadataForClass(ContextDescriptor, 'ContextDescriptor', VOID, VOID, [SerialDescriptor]);
initMetadataForClass(elementDescriptors$1);
initMetadataForClass(elementDescriptors$$inlined$Iterable$1);
initMetadataForClass(ClassSerialDescriptorBuilder, 'ClassSerialDescriptorBuilder');
initMetadataForInterface(CachedNames, 'CachedNames');
protoOf(SerialDescriptorImpl).gg = get_isNullable;
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
protoOf(ListLikeDescriptor).gg = get_isNullable;
initMetadataForClass(ListLikeDescriptor, 'ListLikeDescriptor', VOID, VOID, [SerialDescriptor]);
initMetadataForClass(ArrayClassDesc, 'ArrayClassDesc');
initMetadataForClass(AbstractCollectionSerializer, 'AbstractCollectionSerializer', VOID, VOID, [KSerializer]);
initMetadataForClass(CollectionLikeSerializer, 'CollectionLikeSerializer');
initMetadataForClass(ReferenceArraySerializer, 'ReferenceArraySerializer');
initMetadataForClass(NullableSerializer, 'NullableSerializer', VOID, VOID, [KSerializer]);
initMetadataForClass(SerialDescriptorForNullable, 'SerialDescriptorForNullable', VOID, VOID, [SerialDescriptor, CachedNames]);
protoOf(PluginGeneratedSerialDescriptor).gg = get_isNullable;
initMetadataForClass(PluginGeneratedSerialDescriptor, 'PluginGeneratedSerialDescriptor', VOID, VOID, [SerialDescriptor, CachedNames]);
initMetadataForInterface(GeneratedSerializer, 'GeneratedSerializer', VOID, VOID, [KSerializer]);
initMetadataForObject(StringSerializer, 'StringSerializer', VOID, VOID, [KSerializer]);
initMetadataForObject(BooleanSerializer, 'BooleanSerializer', VOID, VOID, [KSerializer]);
initMetadataForObject(IntSerializer, 'IntSerializer', VOID, VOID, [KSerializer]);
protoOf(PrimitiveSerialDescriptor).gg = get_isNullable;
initMetadataForClass(PrimitiveSerialDescriptor, 'PrimitiveSerialDescriptor', VOID, VOID, [SerialDescriptor]);
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

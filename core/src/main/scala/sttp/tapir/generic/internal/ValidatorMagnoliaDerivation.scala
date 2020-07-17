package sttp.tapir.generic.internal

import com.github.ghik.silencer.silent
import magnolia.{Magnolia, ReadOnlyCaseClass, SealedTrait}
import sttp.tapir.{FieldName, Validator, generic}
import sttp.tapir.generic.Configuration

trait ValidatorMagnoliaDerivation {
  type Typeclass[T] = Validator[T]

  def combine[T](ctx: ReadOnlyCaseClass[Validator, T])(implicit genericDerivationConfig: Configuration): Validator[T] = {
    Validator.Product(ctx.parameters.map { p =>
      p.label -> new Validator.ProductField[T] {
        override type FieldType = p.PType
        override def name: FieldName = FieldName(p.label, genericDerivationConfig.toLowLevelName(p.label))
        override def get(t: T): FieldType = p.dereference(t)
        override def validator: Typeclass[FieldType] = p.typeclass
      }
    }.toMap)
  }

  @silent("never used")
  def dispatch[T](ctx: SealedTrait[Validator, T]): Validator[T] =
    Validator.Coproduct(new generic.SealedTrait[Validator, T] {
      override def dispatch(t: T): Typeclass[T] = ctx.dispatch(t) { v => v.typeclass.asInstanceOf[Validator[T]] }

      override def subtypes: Map[String, Typeclass[Any]] =
        ctx.subtypes.map(st => st.typeName.full -> st.typeclass.asInstanceOf[Validator[scala.Any]]).toMap
    })

  implicit def validatorForCaseClass[T]: Validator[T] = macro Magnolia.gen[T]

  def fallback[T]: Validator[T] = Validator.pass
}

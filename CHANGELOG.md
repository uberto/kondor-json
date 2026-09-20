<h2 class="github">Changelog</h2>

This list is not currently intended to be all-encompassing - it will document major and breaking API changes with their
rationale when appropriate:

### v.4.0.2 - 19 September 2026

Kondor-core: fixed parsing of empty Json objects `{}` in JMap and JObj (e.g. empty maps and objects with only null fields)
Kondor-core: a `JObj` missing a mandatory field reports `Not found key` with the field name, like `JAny`, instead of a
generic NullPointerException error; converters handling missing fields themselves can override
`failOnMissingMandatoryFields` (as `JDataClassWithNames` does, to use the constructor default values)
Kondor-core: a `null` value in a `JMap` reports the key (`Found null for non-nullable`) instead of a generic error
Kondor-core: `JObj` ignores unknown Json fields like `JAny` (4.0.0 and 4.0.1 failed on them), so `JDataClass`,
`JInstance` and `JObj` Mongo tables accept the `_id` field; invalid Json in an unknown field is still an error
Kondor-core: `flatten` fields work in a `JObj`, including `flatten` of a `JsonNodeObject`, reading the undeclared fields;
a `flatten` field declared before other fields no longer reads them when parsing a `JObj` from a `JsonNode`
Kondor-core: truncated Json (e.g. `[1, 2` or `{"a": "b`) returns a parsing error (`expected ... but found end of file`)
instead of throwing an `EndOfCollection` exception; a missing field value (e.g. `{"a": }`) reports the token found
instead of `nothing`
Kondor-auto: `JDataClassAuto.registerAllProperties()` is deprecated and does nothing; calling it registered the
properties twice
Kondor-mongo: tests use the multi-arch `mongo:6.0.14` image (works on Apple Silicon), overridable with `MONGO_TEST_IMAGE`
Docs: fixed error-handling examples in README (`fromJson` instead of the non-existent `parseJson`)
Build: release script only updates the version declarations instead of every matching string in README

### v.4.0.0 / v.4.0.1 - Kondor 4 (2025-2026)

Kondor 4 introduces a faster way to parse objects directly from the Json tokens, without building the intermediate
`JsonNode` tree. See [MigrationToV4.md](kondor-core/MigrationToV4.md) for how to upgrade.

New:

Kondor-core: `JObj<T>`, a new object converter parsing directly from tokens. Fields are declared as with `JAny`, and the
deserialization is `override fun FieldsValues.deserializeOrThrow(path: NodePath)`
Kondor-auto: `JDataClassAuto<T>`, a converter for data classes that needs no field declarations at all (uses reflection)
Kondor-auto: `JDataClassWithNames<T>`, binds the constructor parameters by name instead of position, supporting default
and nullable parameters

Breaking changes:

Kondor-core: `JMap`, `JInstance` and `JDataClass` are now based on `JObj`; `JJsonNode` and `JSealed` are based on `JAny`
Kondor-core: `JObj` converters (including `JDataClass` and `JInstance`) fail on unknown Json fields, while `JAny` ignores
them
Kondor-core: `JsonNodeObject._fieldMap` is now a `FieldNodeMap` wrapper (use `_fieldMap.map` for the plain map),
`asObjFieldMap()` returns a `Map<String, JsonNode>` and `convertFields` returns a `FieldNodeMap`
Kondor-core: the experimental 3.x `JObj.deserFieldMapOrThrow(fieldMap)` is replaced by
`FieldsValues.deserializeOrThrow(path)`
Kondor-core: removed the deprecated `checkForJsonTail`
Kondor-auto: removed `JAnyAuto` and `JDataClassReflect`; use `JDataClass` or the new `JDataClassAuto`, which doesn't need
`registerAllProperties`
Kondor-core: several internal classes changed (see the migration notes if you subclass `ObjectNodeConverter` directly)

Performance:

Kondor-core: objects parsed with `JObj` skip the `JsonNode` intermediate step
Kondor-core: `JsonLexerLazy` (used to parse from an `InputStream`) buffers its input and no longer uses coroutines

Known limitations:

Kondor-core: a `flatten` field in a `JObj` is rendered correctly, but `fromJson` fails on the flattened fields; use `JAny`
for converters with `flatten`

Fixes:

Kondor-core: correct error paths for arrays and nested objects, no duplicated fields in error paths
Kondor-core: nullable fields parsed from tokens, `VersionedConverter` and schema generation work with the new parsing

### v.3.6.1 - 19 October 2025

all: publishing on the new Maven Central Publisher Portal

### v.3.6.0 - 4 October 2025

Kondor-core: strings with unicode control characters are correctly rendered and parsed (thanks altiliano)
Kondor-core: more tests for BigInteger, BigDecimal, NaN and Infinity
Kondor-outcome: documentation and missing tests
Kondor-mongo: tests work on MacOs

### v.3.5.2 - 7 May 2025

Kondor-core: Solidus escaped as Json standard (Thanks Nat Pryce)
Kondor-core: JArray works with non standard collections (Thanks David Wickes)

### v.3.5.1 - 9 April 2025

Kondor-core: short bool funs for JBooleanRepresentable (Thanks David Wickes)

### v.3.5.0 - 29 March 2025

Kondor-core: Better errors for JNum parser exceptions
Kondor-core: Unversioned versions in the VersionedConverter
Kondor-jackson: New module to simplify migration from Jackson-Json (Thanks David Wickes)

### v.3.4.2 - 12 March 2025

Kondor-core: Added dinatural() method to JsonConverter to map a converter over another (thanks Nat Pryce)
Kondor-core: More precise (and faster) base converters for numbers (JLongRepresentable, JDoubleRepresentable, etc)

### v.3.4.1 - 17 February 2025

Kondor-auto: new module with converters using reflection at "test-time"
Kondor-core: fixed pretty rendering issues (thanks Nat Pryce)
Kondor-core: improved parsing performance on arrays

### v.3.3.0 - 27 December 2024

Kondor-core: updated to Kotlin 2.1.0
Kondor-mongo: updated Mongo driver to 5.1.0
Kondor-mongo: added MongoExecutor.fromClientSettings constructor

### v.3.2.1 - 25 September 2024
Kondor-core: fixed pretty rendering of arrays

### v.3.2.0 - 5 September 2024
Kondor-mongo: added support for BulkWrite operations

### v.3.1.2 - 31 August 2024
all: switched to Kotlin scripts and version catalog (thanks André Martins)

### v.3.1.1 - 22 August 2024

Kondor-core: added JFloat to support Float values (thanks @nilshelmig)
Kondor-core: num accepts custom JNumRepresentable (thanks @nilshelmig)
Kondor-core: added UUID converter
Kondor-mongo: fromBsonDoc return an Outcome error instead of nullable for better errors
Kondor-mongo: made internalRun visible on MongoSession for accessing Java driver features

### v.3.0.0 - 31 May 2024

Kondor-core: removing the path from the JsonNode object and instead passing it during parsing phase for performance reasons

### v.2.3.3 - 22 May 2024

Kondor-core: better exception messages and arrays pretty rendering

### v.2.3.2 - 11 April 2024

Kondor-core: added VersionedConverter to handle Json migrations (thanks N.Pryce)

### v.2.3.1 - 11 April 2024

Kondor-core: fixed a bug on Array style

### v.2.3.0 - 10 April 2024

Kondor-core: added NdJson utility functions
Kondor-core: added toJsonStream function to write to output stream
Kondor-mongo: added findByOid and bindOutcome functions

### v.2.2.3 - 14 February 2024

Kondor-core: trying to parse an empty file gives an error

### v.2.2.2 - 31 January 2024

Kondor-core: handling NaN and Infinity deserialization of Double

### v.2.2.1 - 10 January 2024

Kondor-core: fixed issue with invalid Json parsing

### v.2.2.0 - 9 January 2024

Kondor-core: added direct render to Json for better performance
Kondor-core: handling NaN and Infinity serialization of Double

### v.2.1.3 - 29 December 2023

Kondor-core: serialization performance improvements
Kondor-core: added short functions for array of Strings

### v.2.1.2 - 23 October 2023

Kondor-core: Allow for optional escaping of forward slash (thanks D.McGregor)

### v.2.1.1 - 21 September 2023

Kondor-core: Performance improvements

### v.2.1.0 - 30 August 2023

Kondor-mongo: Handle correctly MongoDb ObjectId type
Kondor-mongo: Renamed MongoReader to MongoOperation

### v.2.0.0 - 14 July 2023

Kondor-core: Added Serializer with multiple options (thanks to Fred Nordin)
Kondor-core: Made compact the default mode
Kondor-mongo: Added Audit
Kondor-mongo: Added infix functions for filters
Kondor-mongo: Added README

### v.1.9.5 - 28 June 2023

Kondor-mongo: added more methods to modify collections

### v.1.9.4 - 20 June 2023

Kondor-mongo: ensureIndex() function

### v.1.9.3 - 9 June 2023

Kondor-core: Added DSL functions to build JsonNode on the fly

### v.1.9.2 - 26 May 2023

Kondor-core: Added JJsonNode field for dynamic nodes mapping
Kondor-mongo: Renamed some functions closer to the mongo driver names

### v.1.9.2 - 26 May 2023

Kondor-core: Added JJsonNode field for dynamic nodes mapping
Kondor-mongo: Renamed some functions closer to the mongo driver names

### v.1.9.1 - 10 May 2023

Kondor-mongo: added findOneAndUpdate, findOneAndReplace, and findOneAndDelete methods
Kondor-outcome: added transformIfNotNull method

### v.1.9.0 - 4 May 2023

all: upgraded to Java 17. Version 1.8.x will stay on Java 8

### v.1.8.5 - 4 May 2023

all: reverted to Java 8 for compatibility
Kondor-cor: Added toCompactJson method for max performance
Kondor-mongo: Introduced MongoExecutor interface and other improvements

### v.1.8.4 - 17 April 2023

Making sure to call onConnection only once for connection to Mongo

### v.1.8.3 - 17 April 2023

Added castOrFail to Outcome

### v.1.8.2 - 13 April 2023

Fixed the package name of kondor-mongo
Added TestContainer for Mongo integration tests

### v.1.8.1 - 12 April 2023

Put the discriminator field in polymorphic Json at the top, because some deserializer can only read it in that position
Updated to Kotlin 1.8.20
Added kondor-mongo module

### v.1.8.0 - 25 November 2022

Changed the path property of JsonNodeObject to avoid clash with Json fields called "path"
Updated to Kotlin 1.7.20

### v.1.7.7 - 2 April 2022

Updated to Kotlin 1.6.10

### v.1.7.6 - 22 February 2022

Better checkes on Int and Long parsing (asad.manji)

### v.1.7.5 - 30 December 2021

Renamed T.failIf to T.asOutcome
Added Outcome.failUnless

### v.1.7.2 - 11 November 2021

Allow to specify a default converter for JSealed

### v.1.7.1 - 6 November 2021

Allow specify a different Locale from default (niqdev)

### v.1.7.0 - 1 November 2021

Added fromJson working with InputStream
Improved error messages on missing properties

### v1.6.8 - 10 October 2021

JStringRepresentable can work with nullable types

### v1.6.7 - 21 September 2021

Letting Unicode escape sequence to be parsed

### v1.6.6 - 17 September 2021

Added LazyTokenizer for InputStream

Added new methods on Outcome

### v1.6.5 - 20 August 2021

Allow JMap field to be flatten

### v1.6.4 - 16 August 2021

Fix double quotation in JMap

### v1.6.3 - 15 August 2021

Made JMap work with any type representable by a String as key (Alistair O'Neill)

### v1.6.2 - 9 August 2021

Performance improvements for big Json arrays

### v1.6.1 - 10 July 2021

Made JLocalDateTime and JLocalTime using a configurable pattern (Alessandro Ciccimarra)

### v1.6.0 - 9 July 2021

Added Json Schema generation

### v1.5.4 - 7 June 2021

Made JLocalDate using a configurable pattern (Alessandro Ciccimarra)

### v1.5.3 - 1 June 2021

Added Profunctor methods

### v1.5.2 - 27 May 2021

Improved parsing error messaging and internal refactoring

### v1.5.1 - 13 May 2021

Fixed inconsistent package names

OutcomeException copy the error to the message

### v1.5.0 - 8 May 2021

Moved Outcome in a separate module, so that can be used independently Added a code generator function to generate the
converters from data classes

### v1.4.5 - 21 Apr 2021

Moved nullable nodes logic to rendering rather than parsing

### v1.4.4 - 20 Apr 2021

PrettyRender sorts json entries for objects

### v1.4.3 - 10 Apr 2021

Optionally serialize null into json null nodes

### v1.4.2 - 9 Apr 2021

Fixed bug with escaped strings

### v1.4.1 - 2 Apr 2021

Reorganized package names

### v1.4.0 - 27 Mar 2021

Flatten Field type

### v1.3.5 - 21 Mar 2021

Pretty Json renderer

### v1.3.4 - 16 Mar 2021

More concise modes

### v1.3.3 - 14 Mar 2021

Concise mode available for all types

Safer tokenizer

### v1.3.2 - 11 Mar 2021

Better error messages

### v1.3.1 - 9 Mar 2021

Added types from java.time

Experimental new syntax for fields

### v1.3.0 - 6 Mar 2021

Renamed JObject to ObjectNodeConverter and in JSealed typeFieldName to discriminatorFieldName and subtypesJObject to
SubConverters

### v1.2.0 - 4 Mar 2021

Fixed test dependency (thanks to Asad Manji)

Better exceptions messages

### v1.1.0 - 2 Mar 2021

Added JSet and JList Fixed a bug parsing empty array and empty objects

### v1.0.0 - 1 Mar 2021

first public release
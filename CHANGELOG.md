<h2 class="github">Changelog</h2>

This list is not currently intended to be all-encompassing - it will document major and breaking API changes with their
rationale when appropriate:

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
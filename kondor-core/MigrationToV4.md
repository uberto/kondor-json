# Migration Issues to Kondor-Json v.4.x
There could be issues if you derived by intermediate types, look at how similar examples are working

## New and Old Converters

- JAny is now kept for compatibility and for those cases that still need JsonNode intermediate step when parsing
- JObj is the new converter that can parse directly from the tokens
- JSealed is still derived by JAny because of "discriminant node position"
- JSubTypes (TBC) is the new converter for subtyping faster but with the type field always first
- JDataClass (TBC) doesn't need the constructor (like old deserializeOrThrow)
- JDataClassAuto (TBC) doesn't need anything (reflection or code generation)

## Issues To Be Fixed Before Release

JObj.fromJsonNode() -> send the FieldNodeMap as value
JEnum -> java.lang.ClassCastException: class java.lang.String
JStringWrapper -> Java class exception (??why)
flatten -> doesn't work both finding fields and path with JObj
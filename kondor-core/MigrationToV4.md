# Migration Issues to Kondor-Json v.4.x
There could be issues if you derived by intermediate types, look at how similar examples are working

## New and Old Converters

- JAny is now kept for compatibility and for those cases that still need JsonNode intermediate step when parsing
- JObj is the new converter that can parse directly from the tokens
- JSealed is still derived by JAny because of "discriminant node position"
- JSubTypes (TBC) is the new converter for subtyping faster but with the type field always first
- JDataClass doesn't need the constructor (like old deserializeOrThrow)
- JDataClassAuto doesn't need anything (reflection or code generation)

## Issues To Be Fixed Before Release

Fix/Ignore tests on kondor-auto
Take care of all !!!

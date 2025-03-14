# Migration Issues to Kondor-Json v.4.x

There could be issues if you derived by intermediate types, look at how similar examples are working

- JAny is now kept for compatibility, still using JsonNode intermediate when parsing
- JObj is the new one parsing directly the tokens
- JSealed is still derived by JAny because of "discriminant node position"
- JMulti is the new one faste but with type always first and a map of values
- JDataClass doesn't need the constructor
- JAuto doesn't need anything (reflection or code generation)
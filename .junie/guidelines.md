# KondorJson Developer Guidelines

## Project Overview

KondorJson is a Kotlin library for JSON serialization/deserialization that focuses on type safety and explicit JSON
format definition. It uses JsonConverter concept without reflection or annotations.

## Project Structure

- `kondor-core`: Main library module with core functionality
- `kondor-auto`: Automatic converters based on reflection
- `kondor-examples`: Example implementations and usage patterns
- `kondor-mongo`: MongoDB integration module
- `kondor-outcome`: Outcome class for Either/Result patterns and utilities
- `kondor-tools`: Development and testing tools

## Build and Run

1. Build the project:
   ```bash
   ./gradlew build
   ```
2. Run tests:
   ```bash
   ./gradlew test
   ```
3. Generate documentation:
   ```bash
   ./gradlew dokka
   ```

## Testing Guidelines

- Write tests in the corresponding module's `src/test` directory
- Follow the existing test patterns in the codebase
- Use `JFixture` for test data generation
- Ensure both positive and negative test cases are covered

## Best Practices

1. Converter Naming:
    - Prefix converter objects with 'J' (e.g., `JUser` for `User` class)
    - Keep converters in the same package as their domain classes

2. JSON Format:
    - Define explicit field mappings in converters
    - Use snake_case for JSON field names
    - Handle nullable fields appropriately

3. Error Handling:
    - Use `orThrow()` only in tests
    - Handle parsing errors gracefully in production code

## Junie Automation Rules

Junie will automatically:

- Run gradle commands without confirmation
- Execute test suites
- Generate documentation

However, it will require explicit confirmation for:

- Shell command execution
- Git operations
- Any destructive operations

## Additional Resources

- [Official Documentation](README.md)
- [Changelog](CHANGELOG.md)
- [API Documentation](https://uberto.github.io/kondor-json/)
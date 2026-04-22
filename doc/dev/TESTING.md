# Testing

Features of smart-workflow are highly covered by tests 
in order to keep them stable and easy to evolve.

## Unit Tests

Small fast executing tests. 
None of them should call third-party services on the wire or cause long execution times.

## Integration tests

Tests with heavy infrastructure involved, naturally started via Docker @Testcontainers. These are suffixed with Testcontainer.

## End 2 End tests

Tests that call real LLM models. ApiKeys will be required. Automated execution is limited to weekends.

## Mocking
CI unit tests must run fast and independently, but our code naturally interacts with slow remote LLMs so we need a mock.
Instead of actually calling remote service we serve static responses that were captured from a real model interaction.

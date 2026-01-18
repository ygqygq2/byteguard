rootProject.name = "byteguard"

include(
    "byteguard-core",
    "byteguard-cli",
    "byteguard-maven-plugin"
)

// Test fixtures
include("test-fixtures")
include("test-fixtures:test-apps:simple-app")
include("test-fixtures:test-apps:lambda-app")

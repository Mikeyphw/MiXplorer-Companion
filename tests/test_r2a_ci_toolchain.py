"""R2A hosted build: enforce the API37 / AGP9 Gradle relationship."""
from pathlib import Path
import unittest
ROOT = Path(__file__).resolve().parents[1]
class R2ToolchainContract(unittest.TestCase):
    def test_api_and_target_sdk(self):
        app = (ROOT/'app/build.gradle.kts').read_text()
        self.assertIn('compileSdk = 37', app)
        self.assertIn('targetSdk = 35', app)
        self.assertIn('io.github.libxposed:service:102.0.0', app)
    def test_agp9_and_integrated_kotlin(self):
        root=(ROOT/'build.gradle.kts').read_text()
        app=(ROOT/'app/build.gradle.kts').read_text()
        self.assertIn('version "9.2.1"',root)
        self.assertNotIn('org.jetbrains.kotlin.android',root+app)
        self.assertNotIn('kotlinOptions',app)
        self.assertIn('JavaVersion.VERSION_17',app)
    def test_hosted_gradle_sdk(self):
        flow=(ROOT/'.github/workflows/android-ci.yml').read_text()
        wrapper=(ROOT/'gradle/wrapper/gradle-wrapper.properties').read_text()
        self.assertIn('android-37',flow)
        self.assertIn('build-tools;36.0.0',flow)
        self.assertIn("gradle-version: '9.4.1'",flow)
        self.assertIn('gradle-9.4.1-bin.zip',wrapper)
        self.assertIn(':app:assembleDebug :app:testDebugUnitTest :app:lintDebug',flow)
if __name__=='__main__': unittest.main()

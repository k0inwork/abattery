import unittest
from unittest.mock import MagicMock, patch
import sys

# Mock Kivy UI components and Plyer
class MockWidget:
    def __init__(self, **kwargs): pass
    def add_widget(self, widget): pass
    def bind(self, **kwargs): pass

mock_kivy = MagicMock()
sys.modules['kivy'] = mock_kivy
sys.modules['kivy.app'] = MagicMock()
sys.modules['kivy.uix.boxlayout'] = MagicMock()
sys.modules['kivy.uix.boxlayout'].BoxLayout = MockWidget
sys.modules['kivy.uix.label'] = MagicMock()
sys.modules['kivy.uix.label'].Label = MockWidget
sys.modules['kivy.uix.button'] = MagicMock()
sys.modules['kivy.uix.button'].Button = MockWidget
sys.modules['kivy.uix.slider'] = MagicMock()
sys.modules['kivy.uix.slider'].Slider = MockWidget
sys.modules['kivy.clock'] = MagicMock()
sys.modules['kivy.core.audio'] = MagicMock()
sys.modules['plyer'] = MagicMock()

import main

class TestBatteryLogic(unittest.TestCase):
    def setUp(self):
        # Manually initialize BatteryMonitor and mock its attributes
        self.monitor = main.BatteryMonitor()
        self.monitor.label = MagicMock()
        self.monitor.status_label = MagicMock()
        self.monitor.slider = MagicMock()
        self.monitor.slider.value = 20
        self.monitor.sound = MagicMock()
        self.monitor.sound.state = 'stop'

    @patch('main.battery')
    def test_alert_triggers_below_threshold_and_not_charging(self, mock_battery):
        # Simulate battery at 15% and NOT charging
        mock_battery.status = {'percentage': 15, 'is_charging': False}
        self.monitor.monitoring = True

        self.monitor.update_battery_status(0)

        # Verify sound.play() was called
        self.monitor.sound.play.assert_called_once()

    @patch('main.battery')
    def test_alert_does_not_trigger_when_charging(self, mock_battery):
        # Simulate battery at 15% but charging
        mock_battery.status = {'percentage': 15, 'is_charging': True}
        self.monitor.monitoring = True

        self.monitor.update_battery_status(0)

        # Verify sound.play() was NOT called
        self.monitor.sound.play.assert_not_called()

    @patch('main.battery')
    def test_alert_does_not_trigger_above_threshold(self, mock_battery):
        # Simulate battery at 25% (above 20% threshold)
        mock_battery.status = {'percentage': 25, 'is_charging': False}
        self.monitor.monitoring = True

        self.monitor.update_battery_status(0)

        # Verify sound.play() was NOT called
        self.monitor.sound.play.assert_not_called()

if __name__ == '__main__':
    unittest.main()

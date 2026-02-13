from kivy.app import App
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.label import Label
from kivy.uix.button import Button
from kivy.uix.slider import Slider
from kivy.clock import Clock
from kivy.core.audio import SoundLoader
from plyer import battery
import os

class BatteryMonitor(BoxLayout):
    def __init__(self, **kwargs):
        super(BatteryMonitor, self).__init__(**kwargs)
        self.orientation = 'vertical'
        self.padding = 40
        self.spacing = 20

        self.label = Label(text="Battery Level: --%", font_size='32sp')
        self.add_widget(self.label)

        self.status_label = Label(text="Status: Unknown", font_size='18sp')
        self.add_widget(self.status_label)

        self.threshold_label = Label(text="Alert Threshold: 20%", font_size='20sp')
        self.add_widget(self.threshold_label)

        self.slider = Slider(min=0, max=100, value=20, step=1)
        self.slider.bind(value=self.on_slider_value_change)
        self.add_widget(self.slider)

        self.toggle_button = Button(text="Start Monitoring", font_size='20sp', size_hint=(1, 0.4))
        self.toggle_button.bind(on_press=self.toggle_monitoring)
        self.add_widget(self.toggle_button)

        self.note_label = Label(
            text="Note: Monitoring may stop in background on some Android versions.",
            font_size='12sp', color=(0.7, 0.7, 0.7, 1)
        )
        self.add_widget(self.note_label)

        self.monitoring = False
        self.sound = None
        self.load_sound()

        # Initial update
        self.update_battery_status(0)

        # Schedule periodic updates every 10 seconds
        Clock.schedule_interval(self.update_battery_status, 10)

    def load_sound(self):
        """Loads the alert sound."""
        sound_path = 'alert.wav'
        if os.path.exists(sound_path):
            self.sound = SoundLoader.load(sound_path)
        else:
            print(f"Warning: {sound_path} not found.")

    def on_slider_value_change(self, instance, value):
        self.threshold_label.text = f"Alert Threshold: {int(value)}%"

    def update_battery_status(self, dt):
        try:
            status = battery.status
            percentage = status.get('percentage')
            is_charging = status.get('is_charging', False)

            if percentage is not None:
                self.label.text = f"Battery Level: {int(percentage)}%"
                self.status_label.text = f"Status: {'Charging' if is_charging else 'Discharging'}"

                # Only alert if NOT charging and below threshold
                if self.monitoring and not is_charging and percentage < self.slider.value:
                    self.play_alert()
            else:
                self.label.text = "Battery Level: Unknown"
                self.status_label.text = "Status: Unknown"
        except Exception as e:
            self.label.text = "Error reading battery"
            print(f"Error: {e}")

    def toggle_monitoring(self, instance):
        self.monitoring = not self.monitoring
        if self.monitoring:
            self.toggle_button.text = "Stop Monitoring"
            self.toggle_button.background_color = (0.8, 0.2, 0.2, 1)
        else:
            self.toggle_button.text = "Start Monitoring"
            self.toggle_button.background_color = (0.2, 0.8, 0.2, 1)

    def play_alert(self):
        if self.sound:
            if self.sound.state == 'stop':
                self.sound.play()

class BatteryApp(App):
    def build(self):
        self.title = "Battery Monitor"
        return BatteryMonitor()

if __name__ == '__main__':
    BatteryApp().run()

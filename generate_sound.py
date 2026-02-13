import wave
import struct
import math

def create_beep(filename, frequency=440, duration=1.0, sample_rate=44100):
    """Generates a simple beep sound file."""
    num_samples = int(sample_rate * duration)
    # 2 bytes per sample (16-bit PCM)
    amplitude = 32767.0

    with wave.open(filename, 'w') as wav_file:
        wav_file.setnchannels(1)  # Mono
        wav_file.setsampwidth(2) # 2 bytes
        wav_file.setframerate(sample_rate)

        for i in range(num_samples):
            # Sine wave
            value = int(amplitude * math.sin(2.0 * math.pi * frequency * i / sample_rate))
            wav_file.writeframes(struct.pack('<h', value))
    print(f"Generated {filename}")

if __name__ == "__main__":
    create_beep('alert.wav')

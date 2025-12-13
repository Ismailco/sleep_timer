import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  Dimensions,
  GestureResponderEvent,
  PanResponder,
  Platform,
  Pressable,
  StyleSheet,
  Text,
  View,
} from "react-native";
import { VolumeManager } from "react-native-volume-manager";

const MAX_MINUTES = 120;
const MAX_SECONDS = MAX_MINUTES * 60;
const DEFAULT_SECONDS = 0;
const CIRCLE_SIZE = 280;
const KNOB_SIZE = 28;
const SNAP_STEP_DEGREES = 5;

export default function Index() {
  const [totalSeconds, setTotalSeconds] = useState(DEFAULT_SECONDS);
  const [remainingSeconds, setRemainingSeconds] = useState(DEFAULT_SECONDS);
  const [isRunning, setIsRunning] = useState(false);
  const [knobAngle, setKnobAngle] = useState(
    (DEFAULT_SECONDS / MAX_SECONDS) * 360
  );
  const circleRef = useRef<View | null>(null);
  const circleCenter = useRef<{ x: number; y: number } | null>(null);

  const lowerMediaVolume = useCallback(async () => {
    if (Platform.OS === "web") return;
    try {
      await VolumeManager.setVolume(0, {
        type: VolumeManager.TYPE_MUSIC,
        showUI: false,
      });
    } catch (error) {
      console.warn("Failed to lower media volume", error);
    }
  }, []);

  const updateCircleCenter = useCallback(() => {
    if (!circleRef.current) return;

    circleRef.current.measure?.((_x, _y, width, height, pageX, pageY) => {
      circleCenter.current = {
        x: pageX + width / 2,
        y: pageY + height / 2,
      };
    });
  }, []);

  useEffect(() => {
    updateCircleCenter();
    const subscription = Dimensions.addEventListener(
      "change",
      updateCircleCenter
    );

    return () => {
      subscription.remove();
    };
  }, [updateCircleCenter]);

  const handleGesture = useCallback(
    (evt: GestureResponderEvent) => {
      const center = circleCenter.current;
      if (!center) return;

      if (isRunning) {
        setIsRunning(false);
      }

      const { pageX, pageY } = evt.nativeEvent;
      const dx = pageX - center.x;
      const dy = pageY - center.y;
      const angleFromRight = Math.atan2(dy, dx) * (180 / Math.PI);
      const normalizedAngle =
        (angleFromRight + 90 + 360) % 360; /* 0 at the top, clockwise */

      const snappedAngle =
        Math.round(normalizedAngle / SNAP_STEP_DEGREES) * SNAP_STEP_DEGREES;

      const rawSeconds = Math.round((snappedAngle / 360) * MAX_SECONDS);
      const clampedSeconds = Math.max(0, Math.min(rawSeconds, MAX_SECONDS));

      setTotalSeconds(clampedSeconds);
      setRemainingSeconds(clampedSeconds);
    },
    [isRunning]
  );

  const panResponder = useRef(
    PanResponder.create({
      onStartShouldSetPanResponder: () => true,
      onMoveShouldSetPanResponder: () => true,
      onPanResponderGrant: handleGesture,
      onPanResponderMove: handleGesture,
    })
  ).current;

  useEffect(() => {
    if (!isRunning) return;

    const intervalId = setInterval(() => {
      setRemainingSeconds((prev) => {
        if (prev <= 1) {
          clearInterval(intervalId);
          setIsRunning(false);
          setTotalSeconds(0);
          lowerMediaVolume();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(intervalId);
  }, [isRunning, lowerMediaVolume]);

  useEffect(() => {
    if (!isRunning) {
      setRemainingSeconds(totalSeconds);
    }
  }, [totalSeconds, isRunning]);

  useEffect(() => {
    const angle = (totalSeconds / MAX_SECONDS) * 360;
    setKnobAngle(angle);
  }, [totalSeconds]);

  const formattedTime = useMemo(() => {
    const minutes = Math.floor(remainingSeconds / 60);
    const seconds = remainingSeconds % 60;
    return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(
      2,
      "0"
    )}`;
  }, [remainingSeconds]);

  const knobPosition = useMemo(() => {
    const radius = CIRCLE_SIZE / 2 - 14;
    const radians = ((knobAngle - 90) * Math.PI) / 180;
    const center = CIRCLE_SIZE / 2;

    return {
      left: center + radius * Math.cos(radians) - KNOB_SIZE / 2,
      top: center + radius * Math.sin(radians) - KNOB_SIZE / 2,
    };
  }, [knobAngle]);

  const handleStart = () => {
    if (totalSeconds === 0) {
      return;
    }

    if (remainingSeconds === 0) {
      setRemainingSeconds(totalSeconds);
    }
    setIsRunning(true);
  };

  const handlePause = () => setIsRunning(false);

  const progress =
    totalSeconds === 0 ? 0 : remainingSeconds / totalSeconds;

  return (
    <View style={styles.container}>
      <Text style={styles.heading}>Sleep Timer</Text>
      <Text style={styles.helper}>Scroll around the circle to set time</Text>

      <View style={styles.content}>
        <View style={styles.circleWrapper}>
          <View
            ref={circleRef}
            onLayout={updateCircleCenter}
            {...panResponder.panHandlers}
            style={styles.circle}
          >
            <View
              style={[
                styles.progressRing,
                {
                  opacity: totalSeconds === 0 ? 0.2 : 1,
                  borderColor: "#6C63FF",
                },
              ]}
            />
            <View
              style={[
                styles.remainingRing,
                {
                  borderColor: "#B4B4B4",
                  transform: [{ scale: 1 - progress * 0.12 }],
                },
              ]}
            />
            <Text style={styles.timeText}>{formattedTime}</Text>
            <Text style={styles.subLabel}>
              {Math.floor(totalSeconds / 60)} min
            </Text>
            <View style={[styles.knob, knobPosition]} />
          </View>
        </View>

        <View style={styles.controlsRow}>
          <Pressable
            onPress={handlePause}
            disabled={!isRunning}
            style={[
              styles.controlButton,
              styles.secondaryButton,
              !isRunning && styles.disabledButton,
            ]}
          >
            <Text style={styles.controlText}>Pause</Text>
          </Pressable>
          <Pressable
            onPress={handleStart}
            disabled={isRunning || totalSeconds === 0}
            style={[
              styles.controlButton,
              styles.primaryButton,
              (isRunning || totalSeconds === 0) && styles.disabledPrimary,
            ]}
          >
            <Text style={[styles.controlText, styles.primaryText]}>
              Start
            </Text>
          </Pressable>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#0E0F1C",
    alignItems: "center",
    paddingHorizontal: 24,
    paddingTop: 64,
  },
  heading: {
    fontSize: 32,
    fontWeight: "600",
    color: "#FFFFFF",
  },
  helper: {
    marginTop: 8,
    color: "#9DA2C8",
  },
  content: {
    flex: 1,
    width: "100%",
    justifyContent: "space-between",
    paddingBottom: 32,
  },
  circleWrapper: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
  },
  circle: {
    width: CIRCLE_SIZE,
    height: CIRCLE_SIZE,
    borderRadius: CIRCLE_SIZE / 2,
    borderWidth: 2,
    borderColor: "rgba(255,255,255,0.08)",
    justifyContent: "center",
    alignItems: "center",
    position: "relative",
    backgroundColor: "rgba(255,255,255,0.03)",
  },
  progressRing: {
    position: "absolute",
    width: CIRCLE_SIZE - 8,
    height: CIRCLE_SIZE - 8,
    borderRadius: (CIRCLE_SIZE - 8) / 2,
    borderWidth: 5,
  },
  remainingRing: {
    position: "absolute",
    width: CIRCLE_SIZE - 32,
    height: CIRCLE_SIZE - 32,
    borderRadius: (CIRCLE_SIZE - 32) / 2,
    borderWidth: 3,
  },
  timeText: {
    fontSize: 56,
    fontWeight: "600",
    color: "#FFFFFF",
  },
  subLabel: {
    marginTop: 4,
    color: "#9DA2C8",
  },
  knob: {
    position: "absolute",
    width: KNOB_SIZE,
    height: KNOB_SIZE,
    borderRadius: KNOB_SIZE / 2,
    backgroundColor: "#6C63FF",
    borderWidth: 2,
    borderColor: "#FFFFFF",
  },
  controlsRow: {
    flexDirection: "row",
    gap: 16,
    marginTop: 48,
  },
  controlButton: {
    flex: 1,
    paddingVertical: 16,
    borderRadius: 999,
    alignItems: "center",
    borderWidth: 1,
  },
  secondaryButton: {
    borderColor: "rgba(255,255,255,0.2)",
  },
  primaryButton: {
    borderColor: "#6C63FF",
    backgroundColor: "#6C63FF",
  },
  disabledButton: {
    opacity: 0.4,
  },
  disabledPrimary: {
    backgroundColor: "rgba(108, 99, 255, 0.4)",
    borderColor: "rgba(108, 99, 255, 0.4)",
  },
  controlText: {
    fontSize: 16,
    fontWeight: "600",
    color: "#FFFFFF",
  },
  primaryText: {
    color: "#0E0F1C",
  },
});

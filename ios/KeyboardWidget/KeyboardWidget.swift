import WidgetKit
import SwiftUI

// Simple widget to provide quick access to keyboard settings
struct KeyboardWidget: Widget {
    let kind: String = "KeyboardWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: Provider()) { entry in
            KeyboardWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("AI Keyboard")
        .description("Quick access to AI Keyboard settings")
        .supportedFamilies([.systemSmall])
    }
}

struct Provider: TimelineProvider {
    func placeholder(in context: Context) -> SimpleEntry {
        SimpleEntry(date: Date())
    }

    func getSnapshot(in context: Context, completion: @escaping (SimpleEntry) -> ()) {
        let entry = SimpleEntry(date: Date())
        completion(entry)
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<Entry>) -> ()) {
        let entries: [SimpleEntry] = [SimpleEntry(date: Date())]
        let timeline = Timeline(entries: entries, policy: .atEnd)
        completion(timeline)
    }
}

struct SimpleEntry: TimelineEntry {
    let date: Date
}

struct KeyboardWidgetEntryView : View {
    var entry: Provider.Entry

    var body: some View {
        VStack(spacing: 4) {
            Image(systemName: "keyboard")
                .font(.system(size: 24))
                .foregroundColor(.blue)
            
            Text("AI Keyboard")
                .font(.caption)
                .fontWeight(.semibold)
            
            Text("Tap to Setup")
                .font(.caption2)
                .foregroundColor(.secondary)
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
    }
}

struct KeyboardWidget_Previews: PreviewProvider {
    static var previews: some View {
        KeyboardWidgetEntryView(entry: SimpleEntry(date: Date()))
            .previewContext(WidgetPreviewContext(family: .systemSmall))
    }
}

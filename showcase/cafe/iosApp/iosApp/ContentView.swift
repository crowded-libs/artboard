import SwiftUI
import CafeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            // SwiftUI supplies the device-safe bounds. Compose skips its own
            // duplicate safe-area padding in MainViewController.
            .ignoresSafeArea(.keyboard)
            .background(Color(red: 19.0 / 255.0, green: 39.0 / 255.0, blue: 31.0 / 255.0))
    }
}

# MarqueeTextView

MarqueeTextView is a custom Android TextView widget that provides a marquee effect for displaying scrolling text. 

It allows you to create dynamic and eye-catching UI elements for your Android applications, especially useful for displaying long text strings that cannot fit within the available screen space.

## Usage

Using MarqueeTextView is straightforward. Here's an example of how to use it in your Android XML layout:

```xml
<com.github.cm.marqueetextview.MarqueeTextView
android:id="@+id/marqueeTextView"
android:layout_width="match_parent"
android:layout_height="wrap_content"
android:text="Your long text goes here..."
android:singleLine="true" />
```
### Attributes
```xml
    <declare-styleable name="MarqueeTextView">
        <attr name="space" format="dimension" />
        <attr name="speed" format="float" />
    </declare-styleable>
```
The above code sets up the MarqueeTextView with a sample long text and starts the marquee animation so that the text will scroll automatically. 
Adjust the android:text attribute to display your desired text in the MarqueeTextView.
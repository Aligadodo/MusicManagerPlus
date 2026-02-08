import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class SelectableTextWidget extends StatelessWidget {
  final String text;
  final TextStyle? style;
  final TextAlign? textAlign;
  final int? maxLines;
  final TextOverflow? overflow;
  final bool isSelectable;

  const SelectableTextWidget({
    super.key,
    required this.text,
    this.style,
    this.textAlign,
    this.maxLines,
    this.overflow,
    this.isSelectable = true,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onDoubleTap: () {
        Clipboard.setData(ClipboardData(text: text));
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('已复制: ${text.length > 50 ? text.substring(0, 50) + '...' : text}'),
            duration: const Duration(seconds: 2),
          ),
        );
      },
      child: isSelectable
          ? SelectableText(
              text,
              style: style,
              textAlign: textAlign,
              maxLines: maxLines,
            )
          : Text(
              text,
              style: style,
              textAlign: textAlign,
              maxLines: maxLines,
              overflow: overflow,
            ),
    );
  }
}
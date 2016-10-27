package com.example;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.function.BiConsumer;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.servlet.http.Part;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_objdetect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.http.MediaType;
import org.springframework.http.converter.BufferedImageHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class DukerizerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DukerizerApplication.class, args);
	}

	@Bean
	BufferedImageHttpMessageConverter bufferedImageHttpMessageConverter() {
		return new BufferedImageHttpMessageConverter();
	}

	@Autowired
	FaceDetector faceDetector;

	// curl -v -F 'file=@hoge.jpg' http://localhost:8080/dukerize > after.jpg
	@PostMapping(path = "dukerize", produces = { MediaType.IMAGE_JPEG_VALUE,
			MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_GIF_VALUE })
	BufferedImage dukerize(@RequestParam Part file) throws IOException {
		opencv_core.Mat source = opencv_core.Mat
				.createFrom(ImageIO.read(file.getInputStream()));
		faceDetector.detectFaces(source, FaceTranslator::dukerize);
		BufferedImage image = new BufferedImage(source.cols(), source.rows(),
				source.getBufferedImageType());
		source.copyTo(image);
		return image;
	}
}

@Component
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
class FaceDetector {
	@Value("${classifierFile:classpath:/haarcascade_frontalface_default.xml}")
	File classifierFile;

	opencv_objdetect.CascadeClassifier classifier;

	private Logger log = LoggerFactory.getLogger(FaceDetector.class);

	public void detectFaces(opencv_core.Mat source,
			BiConsumer<opencv_core.Mat, opencv_core.Rect> detectAction) {
		opencv_core.Rect faceDetections = new opencv_core.Rect();
		classifier.detectMultiScale(source, faceDetections);
		int numOfFaces = faceDetections.limit();
		log.info("{} faces are detected!", numOfFaces);
		for (int i = 0; i < numOfFaces; i++) {
			opencv_core.Rect r = faceDetections.position(i);
			detectAction.accept(source, r);
		}
	}

	@PostConstruct
	void init() throws IOException {
		if (log.isInfoEnabled()) {
			log.info("load {}", classifierFile.toPath());
		}
		this.classifier = new opencv_objdetect.CascadeClassifier(
				classifierFile.toPath().toString());
	}
}

class FaceTranslator {
	public static void dukerize(opencv_core.Mat source, opencv_core.Rect r) {
		int x = r.x(), y = r.y(), h = r.height(), w = r.width();
		// make the face Duke
		// black upper rectangle
		opencv_core.rectangle(source, new opencv_core.Point(x, y),
				new opencv_core.Point(x + w, y + h / 2),
				new opencv_core.Scalar(0, 0, 0, 0), -1, opencv_core.CV_AA, 0);
		// white lower rectangle
		opencv_core.rectangle(source, new opencv_core.Point(x, y + h / 2),
				new opencv_core.Point(x + w, y + h),
				new opencv_core.Scalar(255, 255, 255, 0), -1, opencv_core.CV_AA, 0);
		// red center circle
		opencv_core.circle(source, new opencv_core.Point(x + h / 2, y + h / 2),
				(w + h) / 12, new opencv_core.Scalar(0, 0, 255, 0), -1, opencv_core.CV_AA,
				0);
	}
}
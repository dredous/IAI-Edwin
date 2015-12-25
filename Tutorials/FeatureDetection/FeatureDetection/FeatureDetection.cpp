// FeatureDetection.cpp : Defines the entry point for the console application.
//

#include "stdafx.h"
#include <highgui.h>
#include <cxcore.h>
#include <opencv2\features2d\features2d.hpp>
#include <opencv2\nonfree\features2d.hpp>
#include <opencv2\nonfree\nonfree.hpp>
#include <opencv2\calib3d\calib3d.hpp>
#include <opencv2\imgproc\imgproc.hpp>
#include <cpprest/http_client.h>
#include <cpprest/filestream.h>
#include <cpprest/json.h>
#include <cpprest/producerconsumerstream.h>
#include <cpprest/containerstream.h>
#include <cv.h>

using namespace cv;
using namespace std;
using namespace web;                        // Common features like URIs.
using namespace web::json;
using namespace web::http;                  // Common HTTP functionality
using namespace web::http::client;

int minHessian = 2000;
cv::Point2f center(0, 0);
Mat frames, pen;
OrbFeatureDetector detector;
OrbDescriptorExtractor extractor;
vector<KeyPoint> keyPoints2;
CvRect calculateBoundingRect(vector<Point2f> keyPoints3);
Point2f computeIntersect(Vec4i a, Vec4i b);
int checkQuadrilateral(vector<Point2f> corners);
void sortCorners(vector<Point2f>&corners, Point2f center);

void detectWithSurf() {
	Mat descriptor1, descriptor2;
	SurfFeatureDetector surf(400);
	SurfDescriptorExtractor extractor;
	vector<KeyPoint> keyPoints1, penKeyPoints;
	surf.detect(frames, keyPoints1);
	surf.detect(pen, penKeyPoints);
	extractor.compute(frames, keyPoints1, descriptor1);
	extractor.compute(pen, penKeyPoints, descriptor2);

	FlannBasedMatcher matcher;
	vector<DMatch> matches;
	matcher.match(descriptor2, descriptor1, matches);
	double max_dist = 0; double min_dist = 100;

	//-- Quick calculation of max and min distances between keypoints
	for (int i = 0; i < descriptor2.rows; i++)
	{
		double dist = matches[i].distance;
		if (dist < min_dist) min_dist = dist;
		if (dist > max_dist) max_dist = dist;
	}

	std::vector< DMatch > good_matches;

	for (int i = 0; i < descriptor2.rows; i++)
	{
		if (matches[i].distance < 3 * min_dist)
		{
			good_matches.push_back(matches[i]);
		}
	}
	Mat imageMatches;
	drawMatches(pen, penKeyPoints, frames, keyPoints1, matches, imageMatches, Scalar::all(-1));

	std::vector<Point2f> obj;
	std::vector<Point2f> scene;

	for (int i = 0; i < good_matches.size(); i++)
	{
		//-- Get the keypoints from the good matches
		obj.push_back(penKeyPoints[good_matches[i].queryIdx].pt);
		scene.push_back(keyPoints1[good_matches[i].trainIdx].pt);
	}

	Mat H = findHomography(obj, scene, CV_RANSAC);

	//-- Get the corners from the image_1 ( the object to be "detected" )
	std::vector<Point2f> obj_corners(4);
	obj_corners[0] = cvPoint(0, 0); obj_corners[1] = cvPoint(pen.cols, 0);
	obj_corners[2] = cvPoint(pen.cols, pen.rows); obj_corners[3] = cvPoint(0, pen.rows);
	std::vector<Point2f> scene_corners(4);

	perspectiveTransform(obj_corners, scene_corners, H);

	//-- Draw lines between the corners (the mapped object in the scene - image_2 )
	line(imageMatches, scene_corners[0] + Point2f(pen.cols, 0), scene_corners[1] + Point2f(pen.cols, 0), Scalar(255, 0, 0), 4);
	line(imageMatches, scene_corners[1] + Point2f(pen.cols, 0), scene_corners[2] + Point2f(pen.cols, 0), Scalar(0, 255, 0), 4);
	line(imageMatches, scene_corners[2] + Point2f(pen.cols, 0), scene_corners[3] + Point2f(pen.cols, 0), Scalar(0, 0, 255), 4);
	line(imageMatches, scene_corners[3] + Point2f(pen.cols, 0), scene_corners[0] + Point2f(pen.cols, 0), Scalar(255, 0, 255), 4);
	Point centerPoint;
	centerPoint.x = scene_corners[0].x + ((scene_corners[1].x - scene_corners[0].x) / 2);
	centerPoint.y = scene_corners[0].y + ((scene_corners[2].y - scene_corners[0].y) / 2);
	putText(imageMatches, "TEST TEXT", centerPoint, FONT_HERSHEY_SIMPLEX, 5, Scalar(255, 255, 0), 3);
	//CvRect boundRect = calculateBoundingRect(scene);

	namedWindow("SURF", 1);
	//rectangle(imageMatches, boundRect, Scalar(255, 255, 0), 2, 8, 0);
	imshow("SURF", imageMatches);
	
}

void saveKeypoints(){
	FileStorage fs("keypoints.yml", FileStorage::WRITE);
	vector<KeyPoint> keyPoints1;
	detector.detect(pen, keyPoints1);
	write(fs, "orbkp", keyPoints1);
	fs.release();
}

void loadKeyPoints(){
	FileStorage fs2("keypoints.yml", FileStorage::READ);
	FileNode kptNode = fs2["orbkp"];
	read(kptNode, keyPoints2);
	fs2.release();
	Mat penKeyPoints;
	namedWindow("Pen KP", CV_WINDOW_AUTOSIZE);
	drawKeypoints(pen, keyPoints2, penKeyPoints, Scalar::all(-1), DrawMatchesFlags::NOT_DRAW_SINGLE_POINTS);
	imshow("Pen KP", penKeyPoints);
}

void uploadKeypoints(){
	json::value keyPointArray = json::value::array();
	for (int i = 0; i < keyPoints2.size(); i++) {
		keyPointArray[i][U("X-Coord")] = json::value::number(keyPoints2.at(i).pt.x);
		keyPointArray[i][U("Y-Coord")] = json::value::number(keyPoints2.at(i).pt.y);
		keyPointArray[i][U("Rotation")] = json::value::number(keyPoints2.at(i).angle);
		keyPointArray[i][U("Size")] = json::value::number(keyPoints2.at(i).size);
	}
	http_client client(U("https://api.parse.com/1/classes/Images"));
	http_request request(methods::POST);
	request.headers().add(U("X-Parse-Application-Id"), U("hTu24zAXg8Tti9oK6W1e1uw36RzC4u3Ar2Ba6pXp"));
	request.headers().add(U("X-Parse-REST-API-Key"), U("2IEqi9DkXxdfIe34DYjMZgVh8WZCI7vCNm134coe"));
	request.headers().set_content_type(U("application/json"));
	json::value kpVal;
	kpVal[L"keypoints"] = keyPointArray;
	request.set_body(kpVal);
	client.request(request).then([](http_response response) {
		if (response.status_code() == status_codes::Created) {
			std::wcout << "Succes in Creating new object" << std::endl;
		}
		else {
			std::wcout << "Failed in Creating new object" << std::endl;
		}
	})
		.wait();
}

vector<KeyPoint> downloadKeyPoints(){
	vector<KeyPoint> downloadedKP;
	http_client client(U("https://api.parse.com/1/classes/Images"));
	http_request request(methods::GET);
	request.headers().add(U("X-Parse-Application-Id"), U("hTu24zAXg8Tti9oK6W1e1uw36RzC4u3Ar2Ba6pXp"));
	request.headers().add(U("X-Parse-REST-API-Key"), U("2IEqi9DkXxdfIe34DYjMZgVh8WZCI7vCNm134coe"));
	client.request(request).then([](http_response response) -> pplx::task<json::value> {
		if (response.status_code() == status_codes::OK) {
			std::cout << " Succesfull Connecting to https://api.parse.com" << std::endl;
			return response.extract_json();
		}
		else {
			std::cout << "Unable to connect to https://api.parse.com" << std::endl;
			return pplx::task_from_result(json::value());
		}
	})
		.then([&](pplx::task<json::value> previousTask) {
		try
		{
			web::json::value const& v = previousTask.get();
			if (!v.is_null())
			{
				for (auto iter = v.as_object().cbegin(); iter != v.as_object().cend(); ++iter)
				{
					const utility::string_t &str = iter->first;
					const json::value &val = iter->second;

					for (int count = 0; count < val.size(); count++)
					{
						auto jsonValue = val.at(count);
						//auto kpVal = jsonValue.at(1);
						auto kpValues = jsonValue.at(U("keypoints")).as_array();
						for (int i = 0; i < kpValues.size(); i++) {
							auto kpIter = kpValues[i];
							KeyPoint kp;
							kp.angle = kpIter.at(U("Rotation")).as_double();
							kp.size = kpIter.at(U("Size")).as_double();
							kp.pt.x = kpIter.at(U("X-Coord")).as_double();
							kp.pt.y = kpIter.at(U("Y-Coord")).as_double();
							downloadedKP.push_back(kp);
						}
					}
				}
			}
		}
		catch (http_exception& e)
		{
			std::wcout << e.what() << std::endl;
		}
	})
		.wait();
	
	//print keyPoints
	for (int i = 0; i < downloadedKP.size(); i++) {
		cout << "X Coord: " << downloadedKP.at(i).pt.x << ", Y Coord: " << downloadedKP.at(i).pt.y << ", Rotation: " << downloadedKP.at(i).angle << ", Size: " << downloadedKP.at(i).size << endl;
		cout << "-----------------------------------------" << endl;
	}
	return downloadedKP;
}

CvRect calculateBoundingRect(vector<Point2f> keyPoints3) {
	float x1, x2, y1, y2, width, height;
	CvRect boundRect;
	if (keyPoints3.size() != 0) {
		x1 = keyPoints3.at(0).x;
		x2 = keyPoints3.at(0).x;
		y1 = keyPoints3.at(0).y;
		y2 = keyPoints3.at(0).y;
		for (int i = 0; i < keyPoints3.size(); i++) {
			if (keyPoints3.at(i).x < x1) {
				x1 = keyPoints3.at(i).x;
			}
			if (keyPoints3.at(i).x > x2) {
				x2 = keyPoints3.at(i).x;
			}
			if (keyPoints3.at(i).y < y1) {
				y1 = keyPoints3.at(i).y;
			}
			if (keyPoints3.at(i).x > y2) {
				y2 = keyPoints3.at(i).y;
			}
		}
		width = x2 - x1;
		height = y2 - y1;
	}
	else {
		x1 = 0;
		x2 = 0;
		y1 = 0;
		y2 = 0;
		width = 0;
		height = 0;
	}
	boundRect = Rect(x1+200,y1,width,height);
	return boundRect;
}
//--------------------------------------------------------------------------
void houghTransform() {
	cv::Mat bw;
	cv::cvtColor(frames, bw, CV_BGR2GRAY);
	imshow("Grayscale", bw);
	cv::blur(bw, bw, cv::Size(3, 3));
	cv::Canny(bw, bw, 100, 100, 3);
	imshow("Canny", bw);
	std::vector<cv::Vec4i> lines;
	cv::HoughLinesP(bw, lines, 1, CV_PI / 180, 70, 30, 10);

	// Expand the lines
	for (int i = 0; i < lines.size(); i++)
	{
		cv::Vec4i v = lines[i];
		lines[i][0] = 0;
		lines[i][1] = ((float)v[1] - v[3]) / (v[0] - v[2]) * -v[0] + v[1];
		lines[i][2] = frames.cols;
		lines[i][3] = ((float)v[1] - v[3]) / (v[0] - v[2]) * (frames.cols - v[2]) + v[3];
	}

	std::vector<cv::Point2f> corners;
	for (int i = 0; i < lines.size(); i++)
	{
		for (int j = i + 1; j < lines.size(); j++)
		{
			cv::Point2f pt = computeIntersect(lines[i], lines[j]);
			if (pt.x >= 0 && pt.y >= 0)
				corners.push_back(pt);
		}
	}
	cout << "Corner Size: " << corners.size() << endl;
	std::vector<cv::Point2f> approx;
	cv::approxPolyDP(cv::Mat(corners), approx, cv::arcLength(cv::Mat(corners), true) * 0.02, true);

	if (approx.size() != 4)
	{
		std::cout << "The object is not quadrilateral!" << std::endl;
		return;
	}

	// Get mass center
	for (int i = 0; i < corners.size(); i++)
		center += corners[i];
	center *= (1. / corners.size());

	cv::Mat dst = frames.clone();
	for (int i = 0; i < lines.size(); i++)
	{
		cv::Vec4i v = lines[i];
		cv::line(dst, cv::Point(v[0], v[1]), cv::Point(v[2], v[3]), CV_RGB(0, 255, 0));
	}
	cout << "Center Position: " << center.x << "," << center.y << endl;
	cv::circle(dst, center, 5, CV_RGB(255, 0, 0), 2);
	imshow("Hough", dst);

	sortCorners(corners, center);
	if (corners.size() == 0){
		std::cout << "The corners were not sorted correctly!" << std::endl;
		return;
	}

	// Draw corner points
	cv::circle(dst, corners[0], 3, CV_RGB(255, 0, 0), 2);
	cv::circle(dst, corners[1], 3, CV_RGB(0, 255, 0), 2);
	cv::circle(dst, corners[2], 3, CV_RGB(0, 0, 255), 2);
	cv::circle(dst, corners[3], 3, CV_RGB(255, 255, 255), 2);

	//// Draw mass center
	//cv::circle(dst, center, 3, CV_RGB(255, 255, 0), 2);

	//cv::Mat quad = cv::Mat::zeros(300, 220, CV_8UC3);

	//std::vector<cv::Point2f> quad_pts;
	//quad_pts.push_back(cv::Point2f(0, 0));
	//quad_pts.push_back(cv::Point2f(quad.cols, 0));
	//quad_pts.push_back(cv::Point2f(quad.cols, quad.rows));
	//quad_pts.push_back(cv::Point2f(0, quad.rows));
	//imshow("image", dst);
}

void sortCorners(vector<Point2f>&corners, Point2f center) {
	std::vector<cv::Point2f> top, bot;

	for (int i = 0; i < corners.size(); i++)
	{
		if (corners[i].y < center.y)
			top.push_back(corners[i]);
		else
			bot.push_back(corners[i]);
	}
	cout << "Top Vector Size: " << top.size() << endl;
	cout << "Bot Vector Size: " << bot.size() << endl;
	corners.clear();
	cv::Point2f tl = top[0].x > top[1].x ? top[1] : top[0];
	cv::Point2f tr = top[0].x > top[1].x ? top[0] : top[1];
	cv::Point2f bl = bot[0].x > bot[1].x ? bot[1] : bot[0];
	cv::Point2f br = bot[0].x > bot[1].x ? bot[0] : bot[1];

	corners.clear();
	corners.push_back(tl);
	corners.push_back(tr);
	corners.push_back(br);
	corners.push_back(bl);

	/*if (top.size() == 2 && bot.size() == 2){
		cv::Point2f tl = top[0].x > top[1].x ? top[1] : top[0];
		cv::Point2f tr = top[0].x > top[1].x ? top[0] : top[1];
		cv::Point2f bl = bot[0].x > bot[1].x ? bot[1] : bot[0];
		cv::Point2f br = bot[0].x > bot[1].x ? bot[0] : bot[1];


		corners.push_back(tl);
		corners.push_back(tr);
		corners.push_back(br);
		corners.push_back(bl);
	}*/
}

Point2f computeIntersect(Vec4i a, Vec4i b){
	int x1 = a[0], y1 = a[1], x2 = a[2], y2 = a[3], x3 = b[0], y3 = b[1], x4 = b[2], y4 = b[3];
	float denom;

	if (float d = ((float)(x1 - x2) * (y3 - y4)) - ((y1 - y2) * (x3 - x4)))
	{
		cv::Point2f pt;
		pt.x = ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / d;
		pt.y = ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)) / d;
		return pt;
	}
	else
		return cv::Point2f(-1, -1);
}
//-------------------------------------------------------------------------

int _tmain(int argc, _TCHAR* argv[])
{
	VideoCapture capture(0);
	if (!capture.isOpened()){
		cout << "There's no webcam / camera connected." << endl;
		return -1;
	}
	//frames = imread("card.jpg");
	pen = imread("book.jpg", 1);
	//saveKeypoints();
	namedWindow("Camera Frame", CV_WINDOW_AUTOSIZE);
	//houghTransform();
	//loadKeyPoints();
	//uploadKeypoints();
	//keyPoints2 = downloadKeyPoints();
	while (true){
		capture >> frames;
		detectWithSurf();
		//houghTransform();
		imshow("Camera Frame", frames);
		int key = waitKey(30);
		if (key == 27) break;
		else if (key == 113) minHessian += 10;
		else if (key == 97) minHessian -= 10;
	}
	return 0;
}


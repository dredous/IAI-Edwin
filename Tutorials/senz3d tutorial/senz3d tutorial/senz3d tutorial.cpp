// senz3d tutorial.cpp : Defines the entry point for the console application.
//

#include "stdafx.h"
#include <util_render.h>
#include <util_pipeline.h>
#include <pxcimage.h>

int _tmain(int argc, _TCHAR* argv[])
{
	UtilPipeline pp;
	pp.EnableImage(PXCImage::COLOR_FORMAT_RGB32, 640, 480);
	pp.EnableImage(PXCImage::COLOR_FORMAT_DEPTH, 320, 240);
	if(!pp.Init()){
		return -1;
	}
	UtilRender colorRender(L"Color Render");
	UtilRender depthRender(L"Depth Render");
	while(true){
		pp.AcquireFrame(true);
		PXCImage* rgbImage = pp.QueryImage(PXCImage::IMAGE_TYPE_COLOR);
		PXCImage* depthImage = pp.QueryImage(PXCImage::IMAGE_TYPE_DEPTH);
		if (!colorRender.RenderFrame(rgbImage)) break;
        if (!depthRender.RenderFrame(depthImage)) break;
		pp.ReleaseFrame();
	}
	pp.Close();
	return 0;
}


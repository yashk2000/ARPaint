<img height='120' src="https://user-images.githubusercontent.com/41234408/82045045-09cac580-96cc-11ea-833d-7ad1e3cdaff4.png" align="left" hspace="1" vspace="1">

# ARPaint :paintbrush: 

An android app which uses [Google ARCore](https://developers.google.com/ar) to allow user to paint on a camera feed.

## Features Implemented :fire: 

- Draw anywhere on the canvas. The canvas being the camera feed :camera: 
- Change line thickness :straight_ruler: 
- Change line color :art: 
- Clear the entire screen :pencil2:

## Screenshots :bar_chart: 

<p float="left">
  <img src="https://user-images.githubusercontent.com/41234408/82045593-f9ffb100-96cc-11ea-82c2-d15c2e7d8d2a.png" width="288" />
  <img src="https://user-images.githubusercontent.com/41234408/82045663-17cd1600-96cd-11ea-9e47-57a59aaf2d62.png" width="288" /> 
  <img src="https://user-images.githubusercontent.com/41234408/82045729-36331180-96cd-11ea-9768-0be3bc326183.png" width="288" />
   <img src="https://user-images.githubusercontent.com/41234408/82045795-52cf4980-96cd-11ea-951b-fa34967cc7be.png" width="288" />
  <img src="https://user-images.githubusercontent.com/41234408/82045957-9b870280-96cd-11ea-906b-ae3ad71a88cb.png" width="288" /> 
  <img src="https://user-images.githubusercontent.com/41234408/82046031-c6715680-96cd-11ea-9b16-378adedaebb1.png" width="288" />
</p>

***Disclaimer: Do not mind the drawings in the screenshots, I'm not good at drawing.***

## Development Setup :triangular_ruler:

Before you begin, you should have already downloaded the Android Studio SDK and set it up correctly. You can find a guide on how to do this here: [Setting up Android Studio](http://developer.android.com/sdk/installing/index.html?pkg=studio)

### Setting up the Android Project :black_nib:

1. Download the project source. You can do this either by forking and cloning the repository (recommended if you plan on pushing changes) or by downloading it as a ZIP file and extracting it.

2. Install the NDK in Android Studio.

3. Open Android Studio, you will see a **Welcome to Android** window. Under Quick Start, select *Import Project (Eclipse ADT, Gradle, etc.)*

4. Navigate to the directory where you saved the ARPaint project, select the root folder of the project (the folder named "ARPaint"), and hit OK. Android Studio should now begin building the project with Gradle.

5. Once this process is complete and Android Studio opens, check the Console for any build errors.

    - *Note:* If you receive a Gradle sync error titled, "failed to find ...", you should click on the link below the error message (if available) that says *Install missing platform(s) and sync project* and allow Android studio to fetch you what is missing.

7. You will also need to setup Android NDK. If a build error due to NDK persists, try replacing ndk with ndk version 20 from [ndk-archives](https://developer.android.com/ndk/downloads/older_releases). Once all build errors have been resolved, you should be all set to build the app and test it.

8. To Build the app, go to *Build > Make Project* (or alternatively press the Make Project icon in the toolbar).

9. If the app was built successfully, you can test it by running it on either a real device or an emulated one by going to *Run > Run 'app'* or pressing the Run icon in the toolbar.

## Learning about ARCore :mortar_board:

Here are a few resources you can use to learn about ARCore:

- [https://developers.google.com/ar](https://developers.google.com/ar)
- [Google Codelabs](https://codelabs.developers.google.com/)
- [https://developers.google.com/ar/develop/java/quickstart](https://developers.google.com/ar/develop/java/quickstart)

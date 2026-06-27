Pod::Spec.new do |s|
  s.name = 'AinuoFaceSDK'
  s.version = '0.1.9'
  s.summary = 'Ainuo Face iOS SDK binary distribution.'
  s.description = 'Ainuo Face SDK XCFramework and bundled runtime resources.'
  s.homepage = 'https://ainuo.com'
  s.license = { :type => 'Commercial', :text => 'Commercial license required.' }
  s.author = { 'Ainuo' => 'support@ainuo.com' }
  s.source = { :git => 'https://github.com/ainuoface/face-sdk.git', :tag => 'v0.1.9' }
  s.platform = :ios, '15.1'
  s.swift_version = '5.0'
  s.vendored_frameworks = 'ios/Frameworks/AinuoFaceSDK.xcframework'
  s.resources = 'ios/Frameworks/*.bundle'
end

# Simple test spec for SRI
describe "Basic SRI functionality" do
  it "should handle basic arithmetic" do
    (1 + 2).should == 3
    (5 * 2).should == 10
  end
  
  it "should handle string operations" do
    "hello".length.should == 5
  end
  
  it "should handle array operations" do
    [1, 2, 3].length.should == 3
    [1, 2, 3][0].should == 1
  end
end
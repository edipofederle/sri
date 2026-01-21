describe("A number literal") do
  it("can be a sequence of decimal digits") do
    435.should == 435
  end

  it("supports addition") do
    result = 10 + 5
    result.should == 15
  end

  it("supports subtraction") do
    result = 20 - 8
    result.should == 12
  end
end

describe("Math operations") do
  it("multiplies correctly") do
    (6 * 7).should == 42
  end

  it("divides correctly") do
    (100 / 10).should == 10
  end
end
